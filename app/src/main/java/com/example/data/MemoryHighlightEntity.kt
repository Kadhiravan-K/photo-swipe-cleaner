package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_highlights")
data class MemoryHighlightEntity(
    @PrimaryKey val monthKey: String, // e.g., "2026-06"
    val summaryText: String,
    val mostPhotographed: String,
    val storageTrend: String,
    val generatedAt: Long = System.currentTimeMillis()
)
