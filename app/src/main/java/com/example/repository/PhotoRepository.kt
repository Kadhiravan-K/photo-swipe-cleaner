package com.example.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.AnalysisEntity
import com.example.data.MemoryHighlightEntity
import com.example.data.PhotoDao
import com.example.data.PhotoEntity
import com.example.data.PhotoWithAnalysis
import com.example.engine.ImageAnalysisEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao
) {
    private val contentResolver: ContentResolver = context.contentResolver

    val activePhotos: Flow<List<PhotoEntity>> = photoDao.getActivePhotos()
    val activePhotosWithAnalysis: Flow<List<PhotoWithAnalysis>> = photoDao.getActivePhotosWithAnalysis()
    val reviewQueue: Flow<List<PhotoEntity>> = photoDao.getReviewQueue()
    val archivedPhotos: Flow<List<PhotoEntity>> = photoDao.getArchivedPhotos()
    val favoritePhotos: Flow<List<PhotoEntity>> = photoDao.getFavoritePhotos()
    val memoryHighlights: Flow<List<MemoryHighlightEntity>> = photoDao.getMemoryHighlights()

    val totalPhotosCount: Flow<Int> = photoDao.getTotalPhotosCountFlow()
    val remainingPhotosCount: Flow<Int> = photoDao.getRemainingPhotosCountFlow()
    val deletedCandidatesCount: Flow<Int> = photoDao.getDeletedCandidatesCountFlow()
    val confirmedDeletedCount: Flow<Int> = photoDao.getConfirmedDeletedCountFlow()
    val recoveredStorageBytes: Flow<Long?> = photoDao.getRecoveredStorageFlow()
    val reviewedPhotosCount: Flow<Int> = photoDao.getPhotosReviewedCountFlow()

    fun searchPhotos(query: String): Flow<List<PhotoEntity>> {
        return photoDao.searchPhotos("%$query%")
    }

    fun getPhotosByCategory(category: String): Flow<List<PhotoEntity>> {
        return photoDao.getPhotosByCategory(category)
    }

    suspend fun scanLocalMedia() = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.MIME_TYPE
        )

        // Standard images and videos query
        val selection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%' OR ${MediaStore.MediaColumns.MIME_TYPE} LIKE 'video/%'"
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val queryUri = MediaStore.Files.getContentUri("external")

        val cursor: Cursor? = contentResolver.query(
            queryUri,
            projection,
            selection,
            null,
            sortOrder
        )

        val newPhotos = mutableListOf<PhotoEntity>()
        
        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val widthColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val filePath = c.getString(dataColumn) ?: ""
                val fileName = c.getString(nameColumn) ?: "Untitled"
                val dateAdded = c.getLong(dateAddedColumn)
                val size = c.getLong(sizeColumn)
                val width = c.getInt(widthColumn)
                val height = c.getInt(heightColumn)
                val mimeType = c.getString(mimeTypeColumn) ?: "image/jpeg"

                val category = when {
                    mimeType.startsWith("video/") -> "Videos"
                    filePath.lowercase().contains("screenshot") || filePath.lowercase().contains("screen_shot") -> "Screenshots"
                    filePath.lowercase().contains("whatsapp") -> "WhatsApp Images"
                    filePath.lowercase().contains("download") -> "Downloads"
                    filePath.lowercase().contains("dcim/camera") || filePath.lowercase().contains("camera/") -> "Camera Photos"
                    else -> "Camera Photos"
                }

                val contentUri: Uri = if (mimeType.startsWith("video/")) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                val photo = PhotoEntity(
                    id = id,
                    uriString = contentUri.toString(),
                    filePath = filePath,
                    fileName = fileName,
                    dateAdded = dateAdded,
                    size = size,
                    width = width,
                    height = height,
                    mimeType = mimeType,
                    category = category
                )
                newPhotos.add(photo)

                if (newPhotos.size >= 100) {
                    photoDao.insertPhotos(newPhotos)
                    newPhotos.clear()
                }
            }
        }

        if (newPhotos.isNotEmpty()) {
            photoDao.insertPhotos(newPhotos)
        }

        analyzeAllUnanalyzed()
    }

    suspend fun analyzeAllUnanalyzed() = withContext(Dispatchers.IO) {
        val unanalyzed = photoDao.getUnanalyzedPhotos()
        Log.d("PhotoRepository", "Analyzing ${unanalyzed.size} photos locally...")
        
        unanalyzed.forEach { photo ->
            try {
                val uri = Uri.parse(photo.uriString)
                val analysisResult = ImageAnalysisEngine.analyzeImage(context, uri, photo.filePath)
                
                if (analysisResult != null) {
                    val entity = AnalysisEntity(
                        photoId = photo.id,
                        blurScore = analysisResult.blurScore,
                        sharpnessScore = analysisResult.sharpnessScore,
                        brightnessScore = analysisResult.brightnessScore,
                        duplicateSimilarityHash = analysisResult.duplicateHash,
                        screenshotProbabilityScore = analysisResult.screenshotProbability,
                        isAnalyzed = true
                    )
                    photoDao.insertAnalysis(entity)
                }
            } catch (e: Exception) {
                Log.e("PhotoRepository", "Failed analyzing ${photo.fileName}", e)
            }
        }
    }

    suspend fun updatePhotoSwipeAction(id: Long, action: SwipeAction) {
        val photo = photoDao.getPhotoWithAnalysisById(id)?.photo ?: return
        when (action) {
            SwipeAction.DELETE -> {
                photoDao.updatePhoto(photo.copy(isDeletedCandidate = true))
            }
            SwipeAction.KEEP -> {
                // Done, keep in active
            }
            SwipeAction.FAVORITE -> {
                photoDao.updatePhoto(photo.copy(isFavorite = true))
            }
            SwipeAction.ARCHIVE -> {
                photoDao.updatePhoto(photo.copy(isArchived = true))
            }
        }
    }

    suspend fun undoAction(id: Long) {
        val photo = photoDao.getPhotoWithAnalysisById(id)?.photo ?: return
        photoDao.updatePhoto(photo.copy(
            isDeletedCandidate = false,
            isArchived = false,
        ))
    }

    suspend fun confirmDeletions(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            val photoWithAnalysis = photoDao.getPhotoWithAnalysisById(id)
            val photo = photoWithAnalysis?.photo
            if (photo != null) {
                photoDao.updatePhoto(photo.copy(reviewConfirmed = true))
                try {
                    val file = File(photo.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("PhotoRepository", "Failed to delete file from path", e)
                }
            }
        }
    }

    suspend fun rejectDeletions(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            val photo = photoDao.getPhotoWithAnalysisById(id)?.photo
            if (photo != null) {
                photoDao.updatePhoto(photo.copy(isDeletedCandidate = false))
            }
        }
    }

    suspend fun saveMemoryHighlight(monthKey: String, summary: String, mostPhotographed: String, storageTrend: String) {
        val highlight = MemoryHighlightEntity(
            monthKey = monthKey,
            summaryText = summary,
            mostPhotographed = mostPhotographed,
            storageTrend = storageTrend
        )
        photoDao.insertMemoryHighlight(highlight)
    }
}

enum class SwipeAction {
    DELETE, KEEP, FAVORITE, ARCHIVE
}
