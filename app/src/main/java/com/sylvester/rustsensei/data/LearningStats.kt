package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_stats")
data class LearningStats(
    @PrimaryKey
    val date: String, // yyyy-MM-dd
    val sectionsRead: Int = 0,
    val exercisesCompleted: Int = 0,
    val studyTimeSeconds: Long = 0
)
