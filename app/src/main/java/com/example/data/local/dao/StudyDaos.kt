package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessionsDirect(): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE isSyncedToFirestore = 0")
    suspend fun getUnsyncedSessions(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Query("UPDATE study_sessions SET isSyncedToFirestore = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_study_logs ORDER BY dateKey DESC")
    fun getAllLogs(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_study_logs ORDER BY dateKey DESC")
    suspend fun getAllLogsDirect(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_study_logs WHERE dateKey = :dateKey LIMIT 1")
    fun getLogByDate(dateKey: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_study_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getLogDirect(dateKey: String): DailyLogEntity?

    @Query("SELECT * FROM daily_study_logs WHERE isSyncedToFirestore = 0")
    suspend fun getUnsyncedLogs(): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLogEntity)

    @Query("UPDATE daily_study_logs SET isSyncedToFirestore = 1 WHERE dateKey = :dateKey")
    suspend fun markSynced(dateKey: String)
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification_progress WHERE id = 1 LIMIT 1")
    fun observeProgress(): Flow<GamificationProgressEntity?>

    @Query("SELECT * FROM gamification_progress WHERE id = 1 LIMIT 1")
    suspend fun getProgress(): GamificationProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: GamificationProgressEntity)

    @Query("UPDATE gamification_progress SET xp = xp + :points, isSyncedToFirestore = 0 WHERE id = 1")
    suspend fun addXp(points: Int)

    @Query("UPDATE gamification_progress SET minutesStudiedToday = minutesStudiedToday + :minutes, totalFocusHours = totalFocusHours + (:minutes / 60.0), isSyncedToFirestore = 0 WHERE id = 1")
    suspend fun addFocusMinutes(minutes: Int)

    @Query("UPDATE gamification_progress SET isSyncedToFirestore = 1, lastSyncTimestamp = :timestamp WHERE id = 1")
    suspend fun markSynced(timestamp: Long)
}

@Dao
interface TaskCompletionDao {
    @Query("SELECT taskId FROM task_completions WHERE dayKey = :dayKey AND isCompleted = 1")
    fun getCompletedTaskIds(dayKey: String): Flow<List<String>>

    @Query("SELECT taskId FROM task_completions WHERE dayKey = :dayKey AND isCompleted = 1")
    suspend fun getCompletedTaskIdsDirect(dayKey: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setTaskCompletion(task: TaskCompletionEntity)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun removeTaskCompletion(taskId: String)

    @Query("DELETE FROM task_completions WHERE dayKey = :dayKey")
    suspend fun clearDayCompletions(dayKey: String)
}
