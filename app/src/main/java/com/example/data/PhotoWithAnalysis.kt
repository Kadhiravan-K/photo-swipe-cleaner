package com.example.data

import androidx.room.Embedded
import androidx.room.Relation

data class PhotoWithAnalysis(
    @Embedded val photo: PhotoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "photoId"
    )
    val analysis: AnalysisEntity?
)
