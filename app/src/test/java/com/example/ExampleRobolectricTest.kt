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
}
