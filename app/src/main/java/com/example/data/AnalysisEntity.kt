package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_analysis")
data class AnalysisEntity(
    @PrimaryKey val photoId: Long,
    val blurScore: Float,
    val sharpnessScore: Float,
    val brightnessScore: Float,
    val duplicateSimilarityHash: String,
    val screenshotProbabilityScore: Float,
    val detectedFacesCount: Int = 0,
    val detectedFaceNames: String = "",
    val extractedText: String = "",
    val isAnalyzed: Boolean = false,
    val analysisTimestamp: Long = System.currentTimeMillis()
)
