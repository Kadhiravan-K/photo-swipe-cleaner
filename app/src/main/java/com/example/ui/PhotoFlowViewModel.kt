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
import com.example.data.AnalysisEntity
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

    // Filter Chips States
    private val _mediaTypeFilter = MutableStateFlow("All")
    val mediaTypeFilter = _mediaTypeFilter.asStateFlow()

    private val _aiFilter = MutableStateFlow("All")
    val aiFilter = _aiFilter.asStateFlow()

    private val _dateFilter = MutableStateFlow("Any Time")
    val dateFilter = _dateFilter.asStateFlow()

    // Active screen navigation
    private val _activeRoute = MutableStateFlow("dashboard")
    val activeRoute: StateFlow<String> = _activeRoute.asStateFlow()

    // Action stack for Undos
    private val actionHistory = mutableListOf<Long>()

    // Customize Settings / Preferences
    private val prefs by lazy {
        PhotoFlowApplication.instance.getSharedPreferences("photoflow_prefs", android.content.Context.MODE_PRIVATE)
    }

    private val _autoAiAnalysisEnabled = MutableStateFlow(prefs.getBoolean("auto_ai_analysis", false))
    val autoAiAnalysisEnabled = _autoAiAnalysisEnabled.asStateFlow()

    private val _swipeLeftEnabled = MutableStateFlow(prefs.getBoolean("swipe_left_enabled", true))
    val swipeLeftEnabled = _swipeLeftEnabled.asStateFlow()

    private val _swipeRightEnabled = MutableStateFlow(prefs.getBoolean("swipe_right_enabled", true))
    val swipeRightEnabled = _swipeRightEnabled.asStateFlow()

    private val _swipeUpEnabled = MutableStateFlow(prefs.getBoolean("swipe_up_enabled", true))
    val swipeUpEnabled = _swipeUpEnabled.asStateFlow()

    private val _swipeDownEnabled = MutableStateFlow(prefs.getBoolean("swipe_down_enabled", true))
    val swipeDownEnabled = _swipeDownEnabled.asStateFlow()

    fun setAutoAiAnalysisEnabled(enabled: Boolean) {
        _autoAiAnalysisEnabled.value = enabled
        prefs.edit().putBoolean("auto_ai_analysis", enabled).apply()
    }

    fun setSwipeLeftEnabled(enabled: Boolean) {
        _swipeLeftEnabled.value = enabled
        prefs.edit().putBoolean("swipe_left_enabled", enabled).apply()
    }

    fun setSwipeRightEnabled(enabled: Boolean) {
        _swipeRightEnabled.value = enabled
        prefs.edit().putBoolean("swipe_right_enabled", enabled).apply()
    }

    fun setSwipeUpEnabled(enabled: Boolean) {
        _swipeUpEnabled.value = enabled
        prefs.edit().putBoolean("swipe_up_enabled", enabled).apply()
    }

    fun setSwipeDownEnabled(enabled: Boolean) {
        _swipeDownEnabled.value = enabled
        prefs.edit().putBoolean("swipe_down_enabled", enabled).apply()
    }

    // Gemini API states
    private val _aiBestShotResult = MutableStateFlow<BestShotResponse?>(null)
    val aiBestShotResult: StateFlow<BestShotResponse?> = _aiBestShotResult.asStateFlow()

    private val _aiHighlightsResult = MutableStateFlow<MemoryHighlightsResponse?>(null)
    val aiHighlightsResult: StateFlow<MemoryHighlightsResponse?> = _aiHighlightsResult.asStateFlow()

    private val _aiTipsResult = MutableStateFlow<List<String>>(emptyList())
    val aiTipsResult: StateFlow<List<String>> = _aiTipsResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- AI Feature 11: Private Vault states ---
    private val _lockedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val lockedPhotoIds = _lockedPhotoIds.asStateFlow()

    fun toggleVaultLock(photoId: Long) {
        val current = _lockedPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _lockedPhotoIds.value = current
        prefs.edit().putStringSet("vault_locked_ids", current.map { it.toString() }.toSet()).apply()
    }

    // 1. Photos stream - combined with search and category filters
    val galleryPhotos: StateFlow<List<PhotoWithAnalysis>> = combine(
        repository.activePhotosWithAnalysis,
        _searchQuery,
        _selectedCategory,
        _mediaTypeFilter,
        _aiFilter,
        _dateFilter,
        _lockedPhotoIds
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val photosWithAnalysis = flows[0] as List<PhotoWithAnalysis>
        val query = flows[1] as String
        val category = flows[2] as String?
        val mediaType = flows[3] as String
        val aiCategory = flows[4] as String
        val dateRange = flows[5] as String
        val locked = flows[6] as Set<Long>

        // Hide locked photos from normal gallery list
        var result = photosWithAnalysis.filter { !locked.contains(it.photo.id) }

        // Apply SharedPreferences category permissions
        val isCamEnabled = prefs.getBoolean("cat_enabled_Camera Photos", true)
        val isScrEnabled = prefs.getBoolean("cat_enabled_Screenshots", true)
        val isWaEnabled = prefs.getBoolean("cat_enabled_WhatsApp Images", true)
        val isDlEnabled = prefs.getBoolean("cat_enabled_Downloads", true)
        val isVidEnabled = prefs.getBoolean("cat_enabled_Videos", true)
        val isFavEnabled = prefs.getBoolean("cat_enabled_Favorites", true)
        val isArcEnabled = prefs.getBoolean("cat_enabled_Archived", true)

        result = result.filter { item ->
            val cat = item.photo.category
            if (!isCamEnabled && cat == "Camera Photos") return@filter false
            if (!isScrEnabled && (cat == "Screenshots" || item.analysis?.screenshotProbabilityScore?.let { it > 0.8f } == true)) return@filter false
            if (!isWaEnabled && cat == "WhatsApp Images") return@filter false
            if (!isDlEnabled && cat == "Downloads") return@filter false
            if (!isVidEnabled && item.photo.mimeType.startsWith("video/")) return@filter false
            if (!isFavEnabled && item.photo.isFavorite) return@filter false
            if (!isArcEnabled && item.photo.isArchived) return@filter false
            true
        }

        // Apply media type filter
        if (mediaType != "All") {
            result = result.filter { item ->
                if (mediaType == "Images") {
                    item.photo.mimeType.startsWith("image/")
                } else {
                    item.photo.mimeType.startsWith("video/")
                }
            }
        }

        // Apply AI / Quality filter
        if (aiCategory != "All") {
            result = result.filter { item ->
                when (aiCategory) {
                    "Blurry" -> item.analysis?.let { ans -> ans.blurScore > 0.6f } == true
                    "Dark/Light" -> item.analysis?.let { ans -> ans.brightnessScore < 0.2f || ans.brightnessScore > 0.8f } == true
                    "Screenshots" -> item.photo.category == "Screenshots" || item.analysis?.let { ans -> ans.screenshotProbabilityScore > 0.8f } == true
                    "With Faces" -> item.analysis?.let { ans -> ans.detectedFacesCount > 0 } == true
                    "OCR / Documents" -> item.analysis?.let { ans -> ans.extractedText.isNotEmpty() } == true
                    else -> true
                }
            }
        }

        // Apply Date filter
        if (dateRange != "Any Time") {
            val currentTimeSeconds = System.currentTimeMillis() / 1000L
            val thresholdSeconds = when (dateRange) {
                "Today" -> currentTimeSeconds - 86450L
                "Last 7 Days" -> currentTimeSeconds - 7 * 86450L
                "Last 30 Days" -> currentTimeSeconds - 30 * 86450L
                else -> 0L
            }
            if (thresholdSeconds > 0) {
                result = result.filter { it.photo.dateAdded >= thresholdSeconds }
            }
        }

        // Apply search query (AI Feature 12: Natural Language Search Support)
        if (query.isNotEmpty()) {
            val q = query.lowercase().trim()
            if (q.contains("blurry") || q.contains("blur")) {
                result = result.filter { it.analysis?.blurScore?.let { b -> b > 0.45f } == true }
            } else if (q.contains("screenshot") || q.contains("capture")) {
                result = result.filter { it.photo.category == "Screenshots" || it.analysis?.screenshotProbabilityScore?.let { s -> s > 0.7f } == true }
            } else if (q.contains("family") || q.contains("group") || q.contains("people") || q.contains("friends")) {
                result = result.filter { (it.analysis?.detectedFacesCount ?: 0) > 1 || (it.analysis?.detectedFaceNames?.lowercase()?.contains("family") == true) }
            } else if (q.contains("best") || q.contains("sharpest") || q.contains("favorite")) {
                result = result.filter { it.photo.isFavorite || calculateDeletionProbability(it) < 0.25f }
            } else if (q.contains("dark") || q.contains("night")) {
                result = result.filter { it.analysis?.brightnessScore?.let { b -> b < 0.25f } == true }
            } else {
                result = result.filter { item ->
                    item.photo.fileName.contains(query, ignoreCase = true) || 
                    formatDate(item.photo.dateAdded).contains(query, ignoreCase = true) ||
                    item.analysis?.extractedText?.contains(query, ignoreCase = true) == true ||
                    item.analysis?.detectedFaceNames?.contains(query, ignoreCase = true) == true ||
                    item.photo.category.contains(query, ignoreCase = true)
                }
            }
        }

        // Apply selectedcategory
        if (category != null) {
            result = result.filter { item ->
                if (category == "Favorites") {
                    item.photo.isFavorite
                } else if (category == "Videos") {
                    item.photo.mimeType.startsWith("video/")
                } else {
                    item.photo.category == category
                }
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Photos with analysis score stream for "Smart Cleanup" ranking
    val smartCleanupPhotos: StateFlow<List<PhotoWithAnalysis>> = combine(
        repository.activePhotosWithAnalysis,
        _searchQuery,
        _selectedCategory,
        _mediaTypeFilter,
        _aiFilter,
        _dateFilter,
        _lockedPhotoIds
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val photosWithAnalysis = flows[0] as List<PhotoWithAnalysis>
        val query = flows[1] as String
        val category = flows[2] as String?
        val mediaType = flows[3] as String
        val aiCategory = flows[4] as String
        val dateRange = flows[5] as String
        val locked = flows[6] as Set<Long>

        // Hide locked photos from smart cleanup list
        var result = photosWithAnalysis.filter { !locked.contains(it.photo.id) }

        // Apply SharedPreferences category permissions
        val isCamEnabled = prefs.getBoolean("cat_enabled_Camera Photos", true)
        val isScrEnabled = prefs.getBoolean("cat_enabled_Screenshots", true)
        val isWaEnabled = prefs.getBoolean("cat_enabled_WhatsApp Images", true)
        val isDlEnabled = prefs.getBoolean("cat_enabled_Downloads", true)
        val isVidEnabled = prefs.getBoolean("cat_enabled_Videos", true)
        val isFavEnabled = prefs.getBoolean("cat_enabled_Favorites", true)
        val isArcEnabled = prefs.getBoolean("cat_enabled_Archived", true)

        result = result.filter { item ->
            val cat = item.photo.category
            if (!isCamEnabled && cat == "Camera Photos") return@filter false
            if (!isScrEnabled && (cat == "Screenshots" || item.analysis?.screenshotProbabilityScore?.let { it > 0.8f } == true)) return@filter false
            if (!isWaEnabled && cat == "WhatsApp Images") return@filter false
            if (!isDlEnabled && cat == "Downloads") return@filter false
            if (!isVidEnabled && item.photo.mimeType.startsWith("video/")) return@filter false
            if (!isFavEnabled && item.photo.isFavorite) return@filter false
            if (!isArcEnabled && item.photo.isArchived) return@filter false
            true
        }

        // Apply media type filter
        if (mediaType != "All") {
            result = result.filter { item ->
                if (mediaType == "Images") {
                    item.photo.mimeType.startsWith("image/")
                } else {
                    item.photo.mimeType.startsWith("video/")
                }
            }
        }

        // Apply AI / Quality filter
        if (aiCategory != "All") {
            result = result.filter { item ->
                when (aiCategory) {
                    "Blurry" -> item.analysis?.let { ans -> ans.blurScore > 0.6f } == true
                    "Dark/Light" -> item.analysis?.let { ans -> ans.brightnessScore < 0.2f || ans.brightnessScore > 0.8f } == true
                    "Screenshots" -> item.photo.category == "Screenshots" || item.analysis?.let { ans -> ans.screenshotProbabilityScore > 0.8f } == true
                    "With Faces" -> item.analysis?.let { ans -> ans.detectedFacesCount > 0 } == true
                    "OCR / Documents" -> item.analysis?.let { ans -> ans.extractedText.isNotEmpty() } == true
                    else -> true
                }
            }
        }

        // Apply Date filter
        if (dateRange != "Any Time") {
            val currentTimeSeconds = System.currentTimeMillis() / 1000L
            val thresholdSeconds = when (dateRange) {
                "Today" -> currentTimeSeconds - 86450L
                "Last 7 Days" -> currentTimeSeconds - 7 * 86450L
                "Last 30 Days" -> currentTimeSeconds - 30 * 86450L
                else -> 0L
            }
            if (thresholdSeconds > 0) {
                result = result.filter { it.photo.dateAdded >= thresholdSeconds }
            }
        }

        if (query.isNotEmpty()) {
            result = result.filter { item ->
                item.photo.fileName.contains(query, ignoreCase = true) ||
                item.analysis?.extractedText?.contains(query, ignoreCase = true) == true ||
                item.analysis?.detectedFaceNames?.contains(query, ignoreCase = true) == true
            }
        }

        if (category != null) {
            result = result.filter { item ->
                if (category == "Favorites") {
                    item.photo.isFavorite
                } else if (category == "Videos") {
                    item.photo.mimeType.startsWith("video/")
                } else {
                    item.photo.category == category
                }
            }
        }

        // Sort by deletion probability
        result = result.sortedByDescending { calculateDeletionProbability(it) }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Vault stream for private locker ---
    val vaultPhotos: StateFlow<List<PhotoWithAnalysis>> = combine(
        repository.activePhotosWithAnalysis,
        _lockedPhotoIds
    ) { photos, locked ->
        photos.filter { locked.contains(it.photo.id) }
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
        // Load locked IDs on launch
        val saved = prefs.getStringSet("vault_locked_ids", emptySet()) ?: emptySet()
        _lockedPhotoIds.value = saved.mapNotNull { it.toLongOrNull() }.toSet()

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

    fun setMediaTypeFilter(filter: String) {
        _mediaTypeFilter.value = filter
    }

    fun setAiFilter(filter: String) {
        _aiFilter.value = filter
    }

    fun setDateFilter(filter: String) {
        _dateFilter.value = filter
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

    fun confirmMultipleDeletions(ids: List<Long>) {
        viewModelScope.launch {
            repository.confirmDeletions(ids)
        }
    }

    fun rejectMultipleDeletions(ids: List<Long>) {
        viewModelScope.launch {
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

    // --- AI Feature 1: Memory Protection System ---
    fun checkMemoryProtection(item: PhotoWithAnalysis): MemoryProtectionInfo? {
        val photo = item.photo
        val analysis = item.analysis

        if (photo.isFavorite) {
            return MemoryProtectionInfo(
                category = "User Starred Favorite",
                confidence = 100,
                explanation = "You manually pinned or favorited this item, which completely locks it against any swiping deletes."
            )
        }

        val lowerName = photo.fileName.lowercase()
        val lowerPath = photo.filePath.lowercase()
        val lowerText = (analysis?.extractedText ?: "").lowercase()
        val lowerFaces = (analysis?.detectedFaceNames ?: "").lowercase()
        val facesCount = analysis?.detectedFacesCount ?: 0

        if (lowerText.contains("birthday") || lowerText.contains("cake") || lowerText.contains("bday") || lowerName.contains("birthday") || lowerName.contains("bday")) {
            return MemoryProtectionInfo(
                category = "Birthday Celebration",
                confidence = 95,
                explanation = "A celebratory birthday event is spotted based on file characteristics or local OCR. AI shields this memory."
            )
        }

        if (lowerText.contains("graduation") || lowerText.contains("diploma") || lowerText.contains("grad") || lowerName.contains("graduation") || lowerName.contains("grad")) {
            return MemoryProtectionInfo(
                category = "Graduation / Milestone",
                confidence = 98,
                explanation = "Academic achievement or milestone indicators found. Highly recommended to keep on device."
            )
        }

        if (lowerText.contains("trip") || lowerText.contains("travel") || lowerText.contains("flight") || lowerText.contains("vacation") || lowerText.contains("beach") || lowerText.contains("hotel") || lowerName.contains("travel") || lowerName.contains("trip") || lowerPath.contains("vacation")) {
            return MemoryProtectionInfo(
                category = "Travel Memoir",
                confidence = 88,
                explanation = "Travel, flight, beach, or scenic terms detected. Retain this memory of exploration."
            )
        }

        if (facesCount > 2) {
            return MemoryProtectionInfo(
                category = "Family / Group Moment",
                confidence = 92,
                explanation = "Contains several active faces ($facesCount) in a portrait layout, representing a cherished family or friendship circle."
            )
        }

        if (lowerFaces.contains("family") || lowerFaces.contains("alice") || lowerFaces.contains("mom") || lowerFaces.contains("dad")) {
            return MemoryProtectionInfo(
                category = "Saved Loved Ones",
                confidence = 94,
                explanation = "Identified close connections (e.g., family group, Alice) inside the photo frame."
            )
        }

        return null
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

data class MemoryProtectionInfo(
    val category: String,
    val confidence: Int,
    val explanation: String
)
