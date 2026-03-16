package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_notes")
data class UserNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectionId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
