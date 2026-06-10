package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE isDeletedCandidate = 0 AND isArchived = 0 AND isSwiped = 0 ORDER BY dateAdded DESC")
    fun getActivePhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isDeletedCandidate = 0 AND isArchived = 0 AND isSwiped = 0 ORDER BY dateAdded DESC")
    suspend fun getActivePhotosList(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE isDeletedCandidate = 1 AND reviewConfirmed = 0 ORDER BY dateAdded DESC")
    fun getReviewQueue(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isDeletedCandidate = 1 AND reviewConfirmed = 0 ORDER BY dateAdded DESC")
    suspend fun getReviewQueueList(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE isArchived = 1 ORDER BY dateAdded DESC")
    fun getArchivedPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoritePhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE category = :category AND isDeletedCandidate = 0 AND isArchived = 0 AND isSwiped = 0 ORDER BY dateAdded DESC")
    fun getPhotosByCategory(category: String): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("UPDATE photos SET isDeletedCandidate = :isDeleted, isArchived = :isArchived, isFavorite = :isFavorite WHERE id = :id")
    suspend fun updatePhotoStatus(id: Long, isDeleted: Boolean, isArchived: Boolean, isFavorite: Boolean)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deletePhotosByIds(ids: List<Long>)

    // --- Analysis Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    @Query("SELECT * FROM photo_analysis WHERE photoId = :photoId")
    suspend fun getAnalysisForPhoto(photoId: Long): AnalysisEntity?

    @Transaction
    @Query("SELECT * FROM photos WHERE isDeletedCandidate = 0 AND isArchived = 0 AND isSwiped = 0 ORDER BY dateAdded DESC")
    fun getActivePhotosWithAnalysis(): Flow<List<PhotoWithAnalysis>>

    @Transaction
    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoWithAnalysisById(id: Long): PhotoWithAnalysis?

    @Query("SELECT * FROM photos WHERE id NOT IN (SELECT photoId FROM photo_analysis WHERE isAnalyzed = 1) AND isDeletedCandidate = 0 AND isArchived = 0 AND isSwiped = 0")
    suspend fun getUnanalyzedPhotos(): List<PhotoEntity>

    // --- Search ---
    @Query("""
        SELECT photos.* FROM photos 
        LEFT JOIN photo_analysis ON photos.id = photo_analysis.photoId 
        WHERE (photos.fileName LIKE :query OR photo_analysis.extractedText LIKE :query OR photo_analysis.detectedFaceNames LIKE :query) 
        AND photos.isDeletedCandidate = 0 AND photos.isArchived = 0 AND photos.isSwiped = 0 
        ORDER BY photos.dateAdded DESC
    """)
    fun searchPhotos(query: String): Flow<List<PhotoEntity>>

    // --- Stats & Dashboard ---

    @Query("SELECT COUNT(*) FROM photos")
    fun getTotalPhotosCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE isDeletedCandidate = 0 AND isArchived = 0 AND isSwiped = 0")
    fun getRemainingPhotosCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE isDeletedCandidate = 1 AND reviewConfirmed = 0")
    fun getDeletedCandidatesCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE reviewConfirmed = 1")
    fun getConfirmedDeletedCountFlow(): Flow<Int>

    @Query("SELECT SUM(size) FROM photos WHERE reviewConfirmed = 1")
    fun getRecoveredStorageFlow(): Flow<Long?>

    @Query("SELECT COUNT(DISTINCT photoId) FROM photo_analysis WHERE isAnalyzed = 1")
    fun getPhotosReviewedCountFlow(): Flow<Int>

    // --- Memory Highlights ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryHighlight(highlight: MemoryHighlightEntity)

    @Query("SELECT * FROM memory_highlights ORDER BY monthKey DESC")
    fun getMemoryHighlights(): Flow<List<MemoryHighlightEntity>>
}
