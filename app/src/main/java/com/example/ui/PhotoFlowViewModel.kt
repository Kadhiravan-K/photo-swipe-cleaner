package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.PhotoFlowApplication
import com.example.api.BestShotResponse
import com.example.api.GeminiClient
import com.example.api.MemoryHighlightsResponse
import com.example.data.PhotoEntity
import com.example.data.PhotoWithAnalysis
import com.example.repository.PhotoRepository
import com.example.repository.SwipeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoFlowViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Active screen navigation
    private val _activeRoute = MutableStateFlow("dashboard")
    val activeRoute: StateFlow<String> = _activeRoute.asStateFlow()

    // Action stack for Undos
    private val actionHistory = mutableListOf<Long>()

    // Gemini API states
    private val _aiBestShotResult = MutableStateFlow<BestShotResponse?>(null)
    val aiBestShotResult: StateFlow<BestShotResponse?> = _aiBestShotResult.asStateFlow()

    private val _aiHighlightsResult = MutableStateFlow<MemoryHighlightsResponse?>(null)
    val aiHighlightsResult: StateFlow<MemoryHighlightsResponse?> = _aiHighlightsResult.asStateFlow()

    private val _aiTipsResult = MutableStateFlow<List<String>>(emptyList())
    val aiTipsResult: StateFlow<List<String>> = _aiTipsResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // 1. Photos stream - combined with search and category filters
    val galleryPhotos: StateFlow<List<PhotoEntity>> = combine(
        repository.activePhotos,
        _searchQuery,
        _selectedCategory
    ) { photos, query, category ->
        var result = photos
        if (query.isNotEmpty()) {
            result = result.filter { 
                it.fileName.contains(query, ignoreCase = true) || 
                formatDate(it.dateAdded).contains(query, ignoreCase = true)
            }
        }
        if (category != null) {
            result = result.filter { it.category == category }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Photos with analysis score stream for "Smart Cleanup" ranking
    val smartCleanupPhotos: StateFlow<List<PhotoWithAnalysis>> = repository.activePhotosWithAnalysis
        .combine(_searchQuery) { list, query ->
            var result = list
            if (query.isNotEmpty()) {
                result = result.filter { 
                    it.photo.fileName.contains(query, ignoreCase = true)
                }
            }
            // Sort by deletion probability
            result.sortedByDescending { calculateDeletionProbability(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Review Queue
    val reviewQueue: StateFlow<List<PhotoEntity>> = repository.reviewQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Counts & Dashboard Metrics
    val totalPhotosCount: StateFlow<Int> = repository.totalPhotosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val remainingPhotosCount: StateFlow<Int> = repository.remainingPhotosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val deletedCandidatesCount: StateFlow<Int> = repository.deletedCandidatesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val confirmedDeletedCount: StateFlow<Int> = repository.confirmedDeletedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reviewedPhotosCount: StateFlow<Int> = repository.reviewedPhotosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recoveredStorageBytes: StateFlow<Long?> = repository.recoveredStorageBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val memoryHighlights = repository.memoryHighlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial scan of photo metadata on launch
        triggerMediaScan()
    }

    fun setRoute(route: String) {
        _activeRoute.value = route
    }

    fun triggerMediaScan() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanLocalMedia()
            } catch (e: Exception) {
                Log.e("PhotoFlowViewModel", "Scan error", e)
            } finally {
                _isScanning.value = false
                Log.d("PhotoFlowViewModel", "Primary scan completed")
            }
        }
    }

    fun analyzePhotos() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.analyzeAllUnanalyzed()
            } catch (e: Exception) {
                Log.e("PhotoFlowViewModel", "On-device analysis error", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun swipeAction(photoId: Long, action: SwipeAction) {
        viewModelScope.launch {
            repository.updatePhotoSwipeAction(photoId, action)
            actionHistory.add(photoId)
        }
    }

    fun undoLastAction() {
        if (actionHistory.isNotEmpty()) {
            val lastId = actionHistory.removeAt(actionHistory.size - 1)
            viewModelScope.launch {
                repository.undoAction(lastId)
            }
        }
    }

    fun confirmAllDeletions() {
        viewModelScope.launch {
            val ids = repository.reviewQueue.first().map { it.id }
            repository.confirmDeletions(ids)
        }
    }

    fun rejectAllDeletions() {
        viewModelScope.launch {
            val ids = repository.reviewQueue.first().map { it.id }
            repository.rejectDeletions(ids)
        }
    }

    fun deleteSingleReviewItem(id: Long) {
        viewModelScope.launch {
            repository.confirmDeletions(listOf(id))
        }
    }

    fun keepSingleReviewItem(id: Long) {
        viewModelScope.launch {
            repository.rejectDeletions(listOf(id))
        }
    }

    // --- Gemini Actions ---

    fun loadAiTips() {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val stats = "Total scanned: ${totalPhotosCount.value}, Deletion Queue: ${deletedCandidatesCount.value}, Reviewed: ${reviewedPhotosCount.value}"
                val tips = GeminiClient.getCleanupTips(stats)
                _aiTipsResult.value = tips
            } catch (e: Exception) {
                Log.e("PhotoFlowViewModel", "AI tips error", e)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runMemoryHighlightsGenerator() {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val stats = """
                    Photos Total: ${totalPhotosCount.value}
                    Reviewed so far: ${reviewedPhotosCount.value}
                    Marked for deletion: ${deletedCandidatesCount.value}
                    Already deleted: ${confirmedDeletedCount.value}
                    Current saved space: ${formatBytes(recoveredStorageBytes.value ?: 0L)}
                """.trimIndent()
                val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                val result = GeminiClient.getMemoryHighlights(currentMonth, stats)
                if (result != null) {
                    _aiHighlightsResult.value = result
                    // Cache to database
                    val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                    repository.saveMemoryHighlight(
                        monthKey = monthKey,
                        summary = result.summary,
                        mostPhotographed = result.mostPhotographed,
                        storageTrend = result.storageTrend
                    )
                }
            } catch (e: Exception) {
                Log.e("PhotoFlowViewModel", "AI memory highlights generation failed", e)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun analyzeSimilarBestShot(photos: List<PhotoWithAnalysis>) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiBestShotResult.value = null
            try {
                // Decode bitmaps locally
                val bitmaps = withContext(Dispatchers.IO) {
                    photos.mapNotNull { item ->
                        try {
                            val uri = Uri.parse(item.photo.uriString)
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = 4 // Downsample for API speed
                            }
                            val stream = PhotoFlowApplication.instance.contentResolver.openInputStream(uri)
                            val bitmap = BitmapFactory.decodeStream(stream, null, options)
                            stream?.close()
                            bitmap
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (bitmaps.isNotEmpty()) {
                    val result = GeminiClient.getBestShotSelection(bitmaps)
                    _aiBestShotResult.value = result
                    bitmaps.forEach { it.recycle() }
                }
            } catch (e: Exception) {
                Log.e("PhotoFlowViewModel", "AI Best Shot Select error", e)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearBestShotResult() {
        _aiBestShotResult.value = null
    }

    // --- Helpers ---

    fun calculateDeletionProbability(item: PhotoWithAnalysis): Float {
        val analysis = item.analysis ?: return 0.0f
        
        var score = 0.0f
        
        // 1. Blur weighting: blurry images heavily penalized
        score += analysis.blurScore * 0.45f
        
        // 2. Exposure check: very dark or highly overexposed is bad
        if (analysis.brightnessScore < 0.2f || analysis.brightnessScore > 0.82f) {
            score += 0.25f
        }
        
        // 3. Is it a screenshot? High probability of being junk / short-lived
        if (analysis.screenshotProbabilityScore > 0.8f) {
            score += 0.3f
        }
        
        return score.coerceIn(0.0f, 1.0f)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
    }

    fun formatDate(timestampSeconds: Long): String {
        return try {
            val date = Date(timestampSeconds * 1000L)
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }
}

class PhotoFlowViewModelFactory(private val repository: PhotoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoFlowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoFlowViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
