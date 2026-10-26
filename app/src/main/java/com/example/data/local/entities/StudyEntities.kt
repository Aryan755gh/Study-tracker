package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val durationMinutes: Int,
    val mode: String, // "focus", "deep_work", "break"
    val soundUsed: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSyncedToFirestore: Boolean = false
)

@Entity(tableName = "daily_study_logs")
data class DailyLogEntity(
    @PrimaryKey val dateKey: String, // "YYYY-MM-DD"
    val topic: String,
    val questionsAttempted: Int,
    val questionsCorrect: Int,
    val accuracy: Int,
    val notes: String,
    val studyMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSyncedToFirestore: Boolean = false
)

@Entity(tableName = "gamification_progress")
data class GamificationProgressEntity(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 180,
    val level: Int = 1,
    val streak: Int = 3,
    val targetMinutes: Int = 240,
    val minutesStudiedToday: Int = 65,
    val totalFocusHours: Float = 18.5f,
    val questionsSolvedTotal: Int = 160,
    val lastActiveDate: String = "",
    val lastSyncTimestamp: Long = 0L,
    val isSyncedToFirestore: Boolean = false
)

@Entity(tableName = "task_completions")
data class TaskCompletionEntity(
    @PrimaryKey val taskId: String,
    val dayKey: String,
    val isCompleted: Boolean = true,
    val completedAt: Long = System.currentTimeMillis()
)
