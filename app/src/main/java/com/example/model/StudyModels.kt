package com.example.model

data class StudyTask(
    val id: String,
    val title: String,
    val meta: String = "",
    val tag: String = "CORE", // CORE, LEARN, PRACTISE, RECALL, TEST
    val durationMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val dayKey: String = ""
)

data class TaskGroup(
    val groupTitle: String,
    val tasks: List<String>
)

data class AcademicDay(
    val id: String,
    val phaseId: String, // "tifr", "week1", "week2", "custom"
    val dateLabel: String,
    val title: String,
    val tag: String,
    val groups: List<TaskGroup>
)

data class AcademicPhase(
    val id: String,
    val name: String,
    val dateRange: String,
    val description: String
)

data class DailyStudyLog(
    val dateKey: String = "",
    val topic: String = "",
    val questionsAttempted: Int = 0,
    val questionsCorrect: Int = 0,
    val accuracy: Int = 0,
    val notes: String = "",
    val studyMinutes: Int = 0
)

data class StudyBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean = false,
    val progress: Float = 1.0f
)

data class GamificationState(
    val xp: Int = 120,
    val streak: Int = 3,
    val targetMinutes: Int = 240,
    val minutesStudiedToday: Int = 65,
    val totalFocusHours: Float = 14.5f,
    val questionsSolvedTotal: Int = 145,
    val badges: List<StudyBadge> = emptyList()
) {
    val level: Int
        get() = when {
            xp < 250 -> 1
            xp < 600 -> 2
            xp < 1200 -> 3
            xp < 2200 -> 4
            xp < 3600 -> 5
            else -> 6
        }

    val rankTitle: String
        get() = when (level) {
            1 -> "Apprentice Scholar"
            2 -> "Deep Thinker"
            3 -> "Focus Strategist"
            4 -> "Research Fellow"
            5 -> "Core Biology Titan"
            else -> "Grandmaster Polymath"
        }

    val xpForCurrentLevel: Int
        get() = when (level) {
            1 -> 0
            2 -> 250
            3 -> 600
            4 -> 1200
            5 -> 2200
            else -> 3600
        }

    val xpForNextLevel: Int
        get() = when (level) {
            1 -> 250
            2 -> 600
            3 -> 1200
            4 -> 2200
            5 -> 3600
            else -> 6000
        }

    val levelProgress: Float
        get() {
            val range = (xpForNextLevel - xpForCurrentLevel).coerceAtLeast(1)
            val current = (xp - xpForCurrentLevel).coerceAtLeast(0)
            return (current.toFloat() / range.toFloat()).coerceIn(0f, 1f)
        }
}

enum class SoundType {
    BINAURAL_ALPHA, // 10 Hz Flow State (200Hz / 210Hz)
    BINAURAL_THETA, // 6 Hz Memory & Deep Learning (180Hz / 186Hz)
    BROWN_NOISE,    // Deep frequency masking, soothing study rumble
    RAIN_WINDOW,    // Gentle rain on window pane
    LOFI_SYNTH,     // Warm procedural lo-fi ambient chord progression
    COZY_LIBRARY    // Quiet library acoustic warmth
}

data class AmbientTrack(
    val type: SoundType,
    val title: String,
    val subtitle: String,
    val tag: String,
    val frequencyLabel: String
)

data class CuratedStudyChannel(
    val id: String,
    val title: String,
    val educator: String,
    val subject: String,
    val description: String,
    val channelUrl: String,
    val searchKeyword: String,
    val recommendedTopics: List<String>
)

data class BlockedDistraction(
    val name: String,
    val category: String,
    val urlPattern: String,
    val isBlocked: Boolean = true
)

data class CalendarSyncEvent(
    val title: String,
    val dateStr: String,
    val startTime: String,
    val durationMin: Int,
    val description: String
)
