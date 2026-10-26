package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.StudyRepository
import com.example.model.GamificationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StudyOS", appName)
  }

  @Test
  fun `verify gamification level progression`() {
    val stateL1 = GamificationState(xp = 100)
    assertEquals(1, stateL1.level)
    assertEquals("Apprentice Scholar", stateL1.rankTitle)

    val stateL3 = GamificationState(xp = 800)
    assertEquals(3, stateL3.level)
    assertEquals("Focus Strategist", stateL3.rankTitle)
  }

  @Test
  fun `verify repository default academic days`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = StudyRepository(context)
    assertTrue(repo.academicDays.value.isNotEmpty())
    assertTrue(repo.todayTasks.value.isNotEmpty())
  }

  @Test
  fun `verify room database and daos persist and sync flags`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.StudyOSDatabase::class.java
    ).allowMainThreadQueries().build()

    val sessionDao = db.studySessionDao()
    val dailyLogDao = db.dailyLogDao()
    val gamificationDao = db.gamificationDao()
    val taskCompletionDao = db.taskCompletionDao()

    // 1. Test Session Dao
    val session = com.example.data.local.entities.StudySessionEntity(
      id = "session_test_123",
      durationMinutes = 25,
      mode = "DEEP_WORK",
      soundUsed = "BINAURAL_ALPHA",
      timestamp = System.currentTimeMillis(),
      isSyncedToFirestore = false
    )
    sessionDao.insertSession(session)

    val unsynced = sessionDao.getUnsyncedSessions()
    assertEquals(1, unsynced.size)
    assertEquals("session_test_123", unsynced[0].id)
    assertEquals(false, unsynced[0].isSyncedToFirestore)

    sessionDao.markSynced(unsynced[0].id)
    val unsyncedAfter = sessionDao.getUnsyncedSessions()
    assertEquals(0, unsyncedAfter.size)

    // 2. Test Daily Log Dao
    val log = com.example.data.local.entities.DailyLogEntity(
      dateKey = "2026-10-26",
      topic = "Molecular Biology & Genetics",
      questionsAttempted = 30,
      questionsCorrect = 27,
      accuracy = 90,
      notes = "Focused on DNA replication enzymes",
      studyMinutes = 120,
      timestamp = System.currentTimeMillis(),
      isSyncedToFirestore = false
    )
    dailyLogDao.insertLog(log)
    val fetchedLog = dailyLogDao.getLogDirect("2026-10-26")
    assertEquals(30, fetchedLog?.questionsAttempted)
    assertEquals(false, fetchedLog?.isSyncedToFirestore)

    dailyLogDao.markSynced("2026-10-26")
    val syncedLog = dailyLogDao.getLogDirect("2026-10-26")
    assertEquals(true, syncedLog?.isSyncedToFirestore)

    // 3. Test Gamification Dao
    val progress = com.example.data.local.entities.GamificationProgressEntity(
      id = 1,
      xp = 350,
      level = 2,
      streak = 5,
      targetMinutes = 240,
      minutesStudiedToday = 60,
      totalFocusHours = 12.5f,
      questionsSolvedTotal = 85,
      lastActiveDate = "2026-10-26",
      lastSyncTimestamp = System.currentTimeMillis(),
      isSyncedToFirestore = false
    )
    gamificationDao.insertOrUpdate(progress)
    val savedProgress = gamificationDao.getProgress()
    assertEquals(350, savedProgress?.xp)
    assertEquals(2, savedProgress?.level)
    assertEquals(false, savedProgress?.isSyncedToFirestore)

    gamificationDao.markSynced(System.currentTimeMillis())
    assertEquals(true, gamificationDao.getProgress()?.isSyncedToFirestore)

    // 4. Test Task Completion Dao
    taskCompletionDao.setTaskCompletion(
      com.example.data.local.entities.TaskCompletionEntity(
        taskId = "task_tifr_1",
        dayKey = "day_1",
        isCompleted = true,
        completedAt = System.currentTimeMillis()
      )
    )
    val completed = taskCompletionDao.getCompletedTaskIdsDirect("day_1")
    assertTrue(completed.contains("task_tifr_1"))

    db.close()
  }

  @Test
  fun `verify repository weekly progress data point aggregation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = StudyRepository(context)
    repo.refreshProgressData()
    kotlinx.coroutines.delay(100)
    val progress = repo.weeklyProgress.value
    assertEquals(14, progress.size)
    assertTrue(progress.last().focusMinutes >= 0)
    assertEquals(240, progress.last().targetMinutes)
  }
}
