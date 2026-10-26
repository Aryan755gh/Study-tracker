package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientAudioSynthesizer
import com.example.calendar.CalendarSyncManager
import com.example.data.StudyRepository
import com.example.firebase.FirebaseAuthManager
import com.example.firebase.FirebaseFirestoreRepository
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    TODAY,
    CALENDAR,
    POMODORO,
    STUDY_MODE,
    GAMIFICATION
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    val repository = StudyRepository(application)
    val audioSynthesizer = AmbientAudioSynthesizer()
    val authManager = FirebaseAuthManager(application)
    val firestoreRepository = FirebaseFirestoreRepository(application)
    val aiCoach = com.example.ai.StudyAiCoach()

    private val _aiCoachState = MutableStateFlow<com.example.ai.AiCoachState>(com.example.ai.AiCoachState.Idle)
    val aiCoachState = _aiCoachState.asStateFlow()

    fun askAiCoach(topic: String, question: String) {
        if (question.isBlank()) return
        _aiCoachState.value = com.example.ai.AiCoachState.Loading
        viewModelScope.launch {
            try {
                val answer = aiCoach.explainConcept(topic, question)
                _aiCoachState.value = com.example.ai.AiCoachState.Success(answer)
                repository.addXp(15, "AI Academic Consultation")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiCoachState.value = com.example.ai.AiCoachState.Error(e.localizedMessage ?: "AI consultation failed.")
            }
        }
    }

    fun analyzeMistakesWithAi(topic: String, notes: String) {
        if (notes.isBlank()) return
        _aiCoachState.value = com.example.ai.AiCoachState.Loading
        viewModelScope.launch {
            try {
                val analysis = aiCoach.analyzeMistakes(topic, notes)
                _aiCoachState.value = com.example.ai.AiCoachState.Success(analysis)
                repository.addXp(25, "AI Mistake Analysis [K/R/A/D/S]")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiCoachState.value = com.example.ai.AiCoachState.Error(e.localizedMessage ?: "AI mistake analysis failed.")
            }
        }
    }

    fun dismissAiState() {
        _aiCoachState.value = com.example.ai.AiCoachState.Idle
    }

    // Navigation (Default to DASHBOARD)
    private val _currentTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val currentTab = _currentTab.asStateFlow()

    // Pomodoro Timer State
    private val _timerDurationTotal = MutableStateFlow(25 * 60)
    val timerDurationTotal = _timerDurationTotal.asStateFlow()

    private val _timerSecondsLeft = MutableStateFlow(25 * 60)
    val timerSecondsLeft = _timerSecondsLeft.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _isBreakMode = MutableStateFlow(false)
    val isBreakMode = _isBreakMode.asStateFlow()

    private val _timerCaption = MutableStateFlow("Ready when you are.")
    val timerCaption = _timerCaption.asStateFlow()

    private var timerJob: Job? = null

    // Academic Calendar Filters
    private val _selectedPhaseFilter = MutableStateFlow("all")
    val selectedPhaseFilter = _selectedPhaseFilter.asStateFlow()

    // Study Mode & Blocker State
    private val _isLockModeActive = MutableStateFlow(false)
    val isLockModeActive = _isLockModeActive.asStateFlow()

    private val _lockDurationMinutes = MutableStateFlow(45)
    val lockDurationMinutes = _lockDurationMinutes.asStateFlow()

    private val _activeChannel = MutableStateFlow<CuratedStudyChannel?>(null)
    val activeChannel = _activeChannel.asStateFlow()

    init {
        authManager.attemptAutoSignIn(viewModelScope)
    }

    // Ambient Sound Tracks
    val ambientTracks = listOf(
        AmbientTrack(
            SoundType.LOFI_SYNTH,
            "Lo-Fi Ambient Chords",
            "Relaxing evolving warm progression",
            "Chill",
            "432 Hz"
        ),
        AmbientTrack(
            SoundType.BINAURAL_ALPHA,
            "Binaural Alpha Waves",
            "10 Hz difference for deep flow state",
            "Focus",
            "200 / 210 Hz"
        ),
        AmbientTrack(
            SoundType.BINAURAL_THETA,
            "Binaural Theta Waves",
            "6 Hz difference for memory consolidation",
            "Recall",
            "180 / 186 Hz"
        ),
        AmbientTrack(
            SoundType.BROWN_NOISE,
            "Deep Brown Noise",
            "Smooth acoustic frequency masking",
            "Masking",
            "Sub-rumble"
        ),
        AmbientTrack(
            SoundType.RAIN_WINDOW,
            "Rain on Window",
            "Gentle raindrop rhythm & soft grey noise",
            "Nature",
            "Calming"
        ),
        AmbientTrack(
            SoundType.COZY_LIBRARY,
            "Quiet Library",
            "Subtle room presence and soft hush",
            "Peaceful",
            "Muffled"
        )
    )

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setPhaseFilter(phaseId: String) {
        _selectedPhaseFilter.value = phaseId
    }

    /**
     * Start an integrated study session:
     * - Configures and starts Pomodoro timer
     * - Engages study lock / distraction shield
     * - Plays soothing binaural flow beats
     */
    fun startStudySession(minutes: Int = 25) {
        setTimerPreset(minutes, isBreak = false)
        startTimer()
        _isLockModeActive.value = true
        _lockDurationMinutes.value = minutes
        if (!audioSynthesizer.isPlaying.value) {
            audioSynthesizer.play(SoundType.BINAURAL_ALPHA)
        }
        Toast.makeText(
            getApplication(),
            "Study Session Started ($minutes min) · Shield & Binaural Beats Active",
            Toast.LENGTH_SHORT
        ).show()
    }

    // --- Pomodoro Functions ---
    fun setTimerPreset(minutes: Int, isBreak: Boolean = false) {
        pauseTimer()
        _isBreakMode.value = isBreak
        val seconds = minutes * 60
        _timerDurationTotal.value = seconds
        _timerSecondsLeft.value = seconds
        _timerCaption.value = if (isBreak) "Break time. Rest your eyes." else "Ready when you are."
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        _timerCaption.value = if (_isBreakMode.value) "Recharging mind..." else "Focus on one thing. Ignore the rest."

        timerJob = viewModelScope.launch {
            while (isActive && _timerSecondsLeft.value > 0) {
                delay(1000L)
                _timerSecondsLeft.value -= 1
            }
            if (_timerSecondsLeft.value <= 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
        if (_timerSecondsLeft.value > 0) {
            _timerCaption.value = "Paused. Resume when ready."
        }
    }

    fun resetTimer() {
        pauseTimer()
        _timerSecondsLeft.value = _timerDurationTotal.value
        _timerCaption.value = "Timer reset."
    }

    private fun onTimerFinished() {
        _isTimerRunning.value = false
        vibrateCompletion()

        if (!_isBreakMode.value) {
            val minutesCompleted = _timerDurationTotal.value / 60
            repository.addFocusMinutes(minutesCompleted)

            // 1. Store locally in Room Database first
            repository.recordSessionInRoom(
                durationMinutes = minutesCompleted,
                mode = "focus",
                soundUsed = audioSynthesizer.currentSound.value.name
            )

            _timerCaption.value = "Session complete! +$minutesCompleted XP earned. Take a break!"

            // 2. Sync pending Room records to Firestore
            viewModelScope.launch {
                repository.syncLocalRoomWithFirestore(firestoreRepository)
            }
        } else {
            _timerCaption.value = "Break finished. Time to get back into flow!"
        }
    }

    fun saveTodayLogWithCloudSync(topic: String, questionsAtt: Int, questionsCorr: Int, notes: String) {
        // Saves locally in Room & updates state
        repository.saveTodayLog(topic, questionsAtt, questionsCorr, notes)
        viewModelScope.launch {
            repository.syncLocalRoomWithFirestore(firestoreRepository)
        }
    }

    fun syncLocalRoomToCloud() {
        viewModelScope.launch {
            val count = repository.syncLocalRoomWithFirestore(firestoreRepository)
            Toast.makeText(getApplication(), "Synced $count local records to Cloud Firestore!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrateCompletion() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 350, 150, 350), -1)
            }
        } catch (_: Exception) {}
    }

    // --- Study Mode / Distraction Blocker ---
    fun toggleStudyLockMode(durationMin: Int = 45) {
        if (!_isLockModeActive.value) {
            _lockDurationMinutes.value = durationMin
            _isLockModeActive.value = true
            setTimerPreset(durationMin)
            startTimer()
            if (!audioSynthesizer.isPlaying.value) {
                audioSynthesizer.play(SoundType.BINAURAL_ALPHA)
            }
        } else {
            _isLockModeActive.value = false
        }
    }

    fun emergencyUnlock() {
        _isLockModeActive.value = false
        repository.addXp(-50, "Emergency Break Study Shield (-50 XP)")
        Toast.makeText(getApplication(), "Study Shield Disabled. -50 XP penalty applied.", Toast.LENGTH_SHORT).show()
    }

    fun setActiveChannel(channel: CuratedStudyChannel?) {
        _activeChannel.value = channel
    }

    // --- Google Calendar Sync ---
    fun syncAcademicDayToGoogleCalendar(context: Context, day: AcademicDay) {
        val googleCalId = CalendarSyncManager.getPrimaryGoogleCalendarId(context)
        val startTime = System.currentTimeMillis() + 60 * 60 * 1000L
        val endTime = startTime + 3 * 60 * 60 * 1000L

        val desc = day.groups.joinToString("\n") { g ->
            "${g.groupTitle}:\n" + g.tasks.joinToString("\n") { "• $it" }
        }

        if (googleCalId != null) {
            val uri = CalendarSyncManager.addEventDirectly(
                context = context,
                calendarId = googleCalId,
                title = "${day.title} (${day.tag})",
                description = desc,
                startMillis = startTime,
                endMillis = endTime
            )
            if (uri != null) {
                Toast.makeText(context, "Added '${day.title}' to Google Calendar with reminders!", Toast.LENGTH_LONG).show()
                repository.addXp(30, "Synced with Google Calendar")
                return
            }
        }

        CalendarSyncManager.openAddEventIntent(
            context = context,
            title = "${day.title} (${day.tag})",
            description = desc,
            startMillis = startTime,
            durationMinutes = 180
        )
    }

    fun syncAllSyllabusToGoogleCalendar(context: Context) {
        val days = repository.academicDays.value
        val googleCalId = CalendarSyncManager.getPrimaryGoogleCalendarId(context)

        if (googleCalId != null) {
            var count = 0
            val baseTime = System.currentTimeMillis() + 2 * 60 * 60 * 1000L
            days.take(7).forEachIndexed { i, day ->
                val start = baseTime + i * 24 * 60 * 60 * 1000L
                val end = start + 3 * 60 * 60 * 1000L
                val desc = day.groups.joinToString("\n") { g ->
                    "${g.groupTitle}: " + g.tasks.joinToString("; ")
                }
                val uri = CalendarSyncManager.addEventDirectly(
                    context, googleCalId, "${day.title} (${day.tag})", desc, start, end
                )
                if (uri != null) count++
            }
            Toast.makeText(context, "Synced $count study blocks to Google Calendar!", Toast.LENGTH_LONG).show()
            repository.addXp(100, "Synced Academic Syllabus to Google Calendar")
        } else {
            if (days.isNotEmpty()) {
                syncAcademicDayToGoogleCalendar(context, days.first())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioSynthesizer.stop()
        timerJob?.cancel()
    }
}
