package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: Long,
    val uriString: String,
    val filePath: String,
    val fileName: String,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeletedCandidate: Boolean = false,
    val reviewConfirmed: Boolean = false,
    val isSwiped: Boolean = false,
    val category: String, // "Screenshots", "Camera Photos", "Downloads", "WhatsApp", "Videos", "Other"
    val addedToDatabaseAt: Long = System.currentTimeMillis()
)
