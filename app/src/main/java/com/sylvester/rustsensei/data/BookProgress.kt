package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_progress")
data class BookProgress(
    @PrimaryKey
    val sectionId: String,
    val chapterId: String,
    val readPercent: Float = 0f,
    val isCompleted: Boolean = false,
    val timeSpentSeconds: Long = 0,
    val bookmarked: Boolean = false,
    val lastReadAt: Long = System.currentTimeMillis()
)
