package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_os_prefs", Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayKey: String = dateFormat.format(Date())

    // --- StateFlows ---
    private val _todayTasks = MutableStateFlow<List<StudyTask>>(emptyList())
    val todayTasks = _todayTasks.asStateFlow()

    private val _academicDays = MutableStateFlow<List<AcademicDay>>(emptyList())
    val academicDays = _academicDays.asStateFlow()

    private val _completedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val completedTaskIds = _completedTaskIds.asStateFlow()

    private val _gamification = MutableStateFlow(GamificationState())
    val gamification = _gamification.asStateFlow()

    private val _todayLog = MutableStateFlow(DailyStudyLog(dateKey = todayKey))
    val todayLog = _todayLog.asStateFlow()

    private val _channels = MutableStateFlow<List<CuratedStudyChannel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _blockedDistractions = MutableStateFlow<List<BlockedDistraction>>(emptyList())
    val blockedDistractions = _blockedDistractions.asStateFlow()

    private val _strategyRules = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val strategyRules = _strategyRules.asStateFlow()

    init {
        loadAcademicSyllabus()
        loadDefaultMissionTasks()
        loadCompletedTasks()
        loadGamification()
        loadTodayLog()
        loadCuratedChannels()
        loadDistractionBlockerList()
        loadStrategyRules()
    }

    // --- Mission Tasks ---
    private fun loadDefaultMissionTasks() {
        val defaults = listOf(
            StudyTask(
                id = "concepts",
                title = "Concepts & lecture",
                meta = "Physics Wallah / Debashish Sir · active notes, not passive watching",
                tag = "CORE",
                durationMinutes = 90
            ),
            StudyTask(
                id = "reading",
                title = "Reference reading",
                meta = "Textbook, diagrams, structures and annotation",
                tag = "LEARN",
                durationMinutes = 45
            ),
            StudyTask(
                id = "pyq",
                title = "PYQs & practice",
                meta = "Aim for 20–30 questions; label K/R/A/D/S mistakes",
                tag = "PRACTISE",
                durationMinutes = 75
            ),
            StudyTask(
                id = "revision",
                title = "Revision & error log",
                meta = "Review mistakes and write 3 key takeaways",
                tag = "RECALL",
                durationMinutes = 30
            ),
            StudyTask(
                id = "paper",
                title = "TIFR Biology / JGEEBILS paper",
                meta = "Timed attempt or focused paper question analysis",
                tag = "TEST",
                durationMinutes = 90
            )
        )
        _todayTasks.value = defaults
    }

    private fun loadCompletedTasks() {
        val set = prefs.getStringSet("completed_tasks_$todayKey", emptySet()) ?: emptySet()
        _completedTaskIds.value = set
    }

    fun toggleTaskCompletion(taskId: String) {
        val current = _completedTaskIds.value.toMutableSet()
        val isNowCompleted = if (current.contains(taskId)) {
            current.remove(taskId)
            false
        } else {
            current.add(taskId)
            true
        }
        _completedTaskIds.value = current
        prefs.edit().putStringSet("completed_tasks_$todayKey", current).apply()

        if (isNowCompleted) {
            addXp(25, "Completed Task")
        }
    }

    fun resetTodayTasks() {
        _completedTaskIds.value = emptySet()
        prefs.edit().remove("completed_tasks_$todayKey").apply()
        // also reset today's minutes
        val state = _gamification.value
        _gamification.value = state.copy(minutesStudiedToday = 0)
        saveGamification()
    }

    fun addCustomTask(title: String, tag: String, durationMin: Int) {
        val newTask = StudyTask(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            meta = "Custom target",
            tag = tag,
            durationMinutes = durationMin
        )
        _todayTasks.value = _todayTasks.value + newTask
    }

    // --- Gamification & Stats ---
    private fun loadGamification() {
        val xp = prefs.getInt("user_xp", 180)
        val streak = prefs.getInt("user_streak", 3)
        val targetMin = prefs.getInt("target_minutes", 240)
        val minutesToday = prefs.getInt("minutes_$todayKey", 75)
        val totalHours = prefs.getFloat("total_focus_hours", 18.5f)
        val questionsTotal = prefs.getInt("questions_total", 160)

        val badges = listOf(
            StudyBadge("badge_first", "First Step", "Complete your first Pomodoro session", "🎯", isUnlocked = true),
            StudyBadge("badge_streak_3", "Consistency Champ", "Maintain a 3-day active study streak", "🔥", isUnlocked = streak >= 3),
            StudyBadge("badge_streak_7", "Unstoppable Scholar", "Maintain a 7-day active study streak", "⚡", isUnlocked = streak >= 7),
            StudyBadge("badge_deep_50", "Deep Focus", "Complete a 50-minute deep work session", "🧠", isUnlocked = true),
            StudyBadge("badge_questions_100", "Question Crusher", "Attempt 100+ practice questions", "📚", isUnlocked = questionsTotal >= 100),
            StudyBadge("badge_zen", "Zen Master", "Study with ambient binaural beats", "🎧", isUnlocked = true),
            StudyBadge("badge_shield", "Distraction Slayer", "Finish a study mode session with zero unlocks", "🛡️", isUnlocked = true)
        )

        _gamification.value = GamificationState(
            xp = xp,
            streak = streak,
            targetMinutes = targetMin,
            minutesStudiedToday = minutesToday,
            totalFocusHours = totalHours,
            questionsSolvedTotal = questionsTotal,
            badges = badges
        )
    }

    fun addXp(amount: Int, reason: String = "") {
        val current = _gamification.value
        val updated = current.copy(xp = current.xp + amount)
        _gamification.value = updated
        saveGamification()
    }

    fun addFocusMinutes(minutes: Int) {
        val current = _gamification.value
        val newMinToday = current.minutesStudiedToday + minutes
        val newHoursTotal = current.totalFocusHours + (minutes / 60f)
        val updated = current.copy(
            minutesStudiedToday = newMinToday,
            totalFocusHours = newHoursTotal,
            xp = current.xp + minutes // 1 XP per minute focused
        )
        _gamification.value = updated
        prefs.edit().putInt("minutes_$todayKey", newMinToday).apply()
        saveGamification()
    }

    private fun saveGamification() {
        val s = _gamification.value
        prefs.edit()
            .putInt("user_xp", s.xp)
            .putInt("user_streak", s.streak)
            .putInt("target_minutes", s.targetMinutes)
            .putFloat("total_focus_hours", s.totalFocusHours)
            .putInt("questions_total", s.questionsSolvedTotal)
            .apply()
    }

    // --- Study Log ---
    private fun loadTodayLog() {
        val topic = prefs.getString("log_topic_$todayKey", "") ?: ""
        val qAtt = prefs.getInt("log_q_att_$todayKey", 0)
        val qCorr = prefs.getInt("log_q_corr_$todayKey", 0)
        val acc = prefs.getInt("log_acc_$todayKey", 0)
        val notes = prefs.getString("log_notes_$todayKey", "") ?: ""

        _todayLog.value = DailyStudyLog(
            dateKey = todayKey,
            topic = topic,
            questionsAttempted = qAtt,
            questionsCorrect = qCorr,
            accuracy = acc,
            notes = notes,
            studyMinutes = _gamification.value.minutesStudiedToday
        )
    }

    fun saveTodayLog(topic: String, questionsAtt: Int, questionsCorr: Int, notes: String) {
        val acc = if (questionsAtt > 0) ((questionsCorr.toFloat() / questionsAtt) * 100).toInt() else 0
        val log = DailyStudyLog(
            dateKey = todayKey,
            topic = topic,
            questionsAttempted = questionsAtt,
            questionsCorrect = questionsCorr,
            accuracy = acc,
            notes = notes,
            studyMinutes = _gamification.value.minutesStudiedToday
        )
        _todayLog.value = log
        prefs.edit()
            .putString("log_topic_$todayKey", topic)
            .putInt("log_q_att_$todayKey", questionsAtt)
            .putInt("log_q_corr_$todayKey", questionsCorr)
            .putInt("log_acc_$todayKey", acc)
            .putString("log_notes_$todayKey", notes)
            .apply()

        // Give XP for reflection
        addXp(35, "Daily Log Reflection Saved")
    }

    // --- Strategy Rules ---
    private fun loadStrategyRules() {
        val rules = mapOf(
            "rule1" to (prefs.getBoolean("rule_rule1", false)),
            "rule2" to (prefs.getBoolean("rule_rule2", false)),
            "rule3" to (prefs.getBoolean("rule_rule3", false)),
            "rule4" to (prefs.getBoolean("rule_rule4", false))
        )
        _strategyRules.value = rules
    }

    fun toggleStrategyRule(ruleKey: String) {
        val cur = _strategyRules.value.toMutableMap()
        val currentVal = cur[ruleKey] ?: false
        cur[ruleKey] = !currentVal
        _strategyRules.value = cur
        prefs.edit().putBoolean("rule_$ruleKey", !currentVal).apply()
    }

    // --- Curated Educational YouTube Channels ---
    private fun loadCuratedChannels() {
        _channels.value = listOf(
            CuratedStudyChannel(
                id = "pw_main",
                title = "Physics Wallah",
                educator = "Alakh Pandey & PW Faculty",
                subject = "Comprehensive Sciences & Foundation",
                description = "Complete concept lectures, high-yield structured series, and exam preparation.",
                channelUrl = "https://www.youtube.com/@PhysicsWallah",
                searchKeyword = "Physics Wallah Biology",
                recommendedTopics = listOf("Biomolecules", "Cell Structure", "Genetics", "Plant Physiology")
            ),
            CuratedStudyChannel(
                id = "pw_debashish",
                title = "PW Debashish Sir",
                educator = "Dr. Debashish",
                subject = "Biochemistry & Molecular Biology",
                description = "Primary resource for amino acids, protein biochemistry, enzyme kinetics, and Lineweaver–Burk plots.",
                channelUrl = "https://www.youtube.com/@PhysicsWallah",
                searchKeyword = "Debashish Sir Biochemistry",
                recommendedTopics = listOf("Protein 4-Level Structure", "Michaelis–Menten", "Enzyme Inhibition", "Ramachandran Plot")
            ),
            CuratedStudyChannel(
                id = "gdc_biochem",
                title = "GDC Classes (Biochemistry)",
                educator = "GDC Faculty",
                subject = "Metabolism & Quantitative Applications",
                description = "Use for broader integrated biochemistry, metabolic pathways (Glycolysis, TCA, ETC) and numerical problem solving.",
                channelUrl = "https://www.youtube.com/@GDCeClasses",
                searchKeyword = "GDC Biochemistry",
                recommendedTopics = listOf("Bioenergetics", "Glycolysis Steps", "Metabolic Regulation", "Numerical Applications")
            ),
            CuratedStudyChannel(
                id = "khan_academy",
                title = "Khan Academy",
                educator = "Sal Khan & Specialists",
                subject = "Cell Biology & Organic Chemistry",
                description = "Crystal clear visual walkthroughs of cellular processes, membrane transport, and DNA replication.",
                channelUrl = "https://www.youtube.com/@khanacademy",
                searchKeyword = "Khan Academy Biology",
                recommendedTopics = listOf("Membrane Transport", "Cell Division & Mitosis", "DNA Replication Enzymes", "Transcription")
            ),
            CuratedStudyChannel(
                id = "mit_ocw",
                title = "MIT OpenCourseWare",
                educator = "Professors Lander & Yaffe",
                subject = "Advanced Molecular Biology & Genetics",
                description = "Undergraduate & graduate level rigorous lectures on recombinant DNA, gene regulation, and recombinant techniques.",
                channelUrl = "https://www.youtube.com/@mitocw",
                searchKeyword = "MIT 7.016 Introductory Biology",
                recommendedTopics = listOf("Gene Regulation & Lac Operon", "PCR & Blotting Techniques", "Chromatin Remodeling")
            ),
            CuratedStudyChannel(
                id = "3blue1brown",
                title = "3Blue1Brown",
                educator = "Grant Sanderson",
                subject = "Mathematical Foundations & Visual Intuition",
                description = "Superb visual mathematical intuition for data interpretation, statistics, and neural models.",
                channelUrl = "https://www.youtube.com/@3blue1brown",
                searchKeyword = "3Blue1Brown",
                recommendedTopics = listOf("Calculus of Kinetics", "Bayes Theorem", "Linear Algebra")
            )
        )
    }

    fun addCustomChannel(title: String, educator: String, subject: String, searchKeyword: String) {
        val newChannel = CuratedStudyChannel(
            id = "user_${System.currentTimeMillis()}",
            title = title,
            educator = educator,
            subject = subject,
            description = "Custom study channel added to your StudyOS.",
            channelUrl = "https://www.youtube.com/results?search_query=${searchKeyword.replace(" ", "+")}",
            searchKeyword = searchKeyword,
            recommendedTopics = listOf("Core Concepts", "Practice Questions", "Revision")
        )
        _channels.value = _channels.value + newChannel
    }

    // --- Distraction Blocker List ---
    private fun loadDistractionBlockerList() {
        _blockedDistractions.value = listOf(
            BlockedDistraction("YouTube Shorts", "Short-Form Video", "youtube.com/shorts"),
            BlockedDistraction("Instagram Reels", "Social Media", "instagram.com/reels"),
            BlockedDistraction("TikTok", "Short-Form Video", "tiktok.com"),
            BlockedDistraction("Reddit Feeds", "Endless Scrolling", "reddit.com"),
            BlockedDistraction("X / Twitter Trends", "Social Media", "twitter.com"),
            BlockedDistraction("Netflix & Video Streams", "Entertainment", "netflix.com")
        )
    }

    fun toggleBlockerItem(name: String) {
        val current = _blockedDistractions.value.toMutableList()
        val index = current.indexOfFirst { it.name == name }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(isBlocked = !item.isBlocked)
            _blockedDistractions.value = current
        }
    }

    // --- Academic Syllabus from Attachments ---
    private fun loadAcademicSyllabus() {
        _academicDays.value = listOf(
            // TIFR Week
            AcademicDay(
                id = "tifr_1",
                phaseId = "tifr",
                dateLabel = "Mon · 26 Oct",
                title = "TIFR Paper 1",
                tag = "Diagnostic",
                groups = listOf(
                    TaskGroup("Timed paper", listOf("Attempt one official TIFR Biology/JGEEBILS paper under timed conditions.")),
                    TaskGroup("Analysis", listOf("Check answers and mark incorrect, unanswered and guessed questions.", "Record five weak concepts.", "Revise two high-priority mistakes."))
                )
            ),
            AcademicDay(
                id = "tifr_2",
                phaseId = "tifr",
                dateLabel = "Tue · 27 Oct",
                title = "TIFR Paper 2",
                tag = "Patterns",
                groups = listOf(
                    TaskGroup("Timed paper", listOf("Attempt one paper or the next available official paper.")),
                    TaskGroup("Analysis", listOf("Compare today's mistakes with yesterday's.", "Identify recurring weaknesses and revise the most important concepts."))
                )
            ),
            AcademicDay(
                id = "tifr_3",
                phaseId = "tifr",
                dateLabel = "Wed · 28 Oct",
                title = "TIFR Paper 3",
                tag = "Data interpretation",
                groups = listOf(
                    TaskGroup("Timed paper", listOf("Attempt one paper under timed conditions.")),
                    TaskGroup("Analysis", listOf("Review experimental-design, graph and data-interpretation questions.", "Record concepts you could not apply and revise two of them."))
                )
            ),
            AcademicDay(
                id = "tifr_4",
                phaseId = "tifr",
                dateLabel = "Thu · 29 Oct",
                title = "TIFR Paper 4",
                tag = "Core concepts",
                groups = listOf(
                    TaskGroup("Timed paper", listOf("Attempt one paper under timed conditions.")),
                    TaskGroup("Analysis", listOf("Identify gaps in cell biology, molecular biology and genetics.", "Revise concepts using PW notes and update the error log."))
                )
            ),
            AcademicDay(
                id = "tifr_5",
                phaseId = "tifr",
                dateLabel = "Fri · 30 Oct",
                title = "TIFR Paper 5",
                tag = "Revision",
                groups = listOf(
                    TaskGroup("Timed paper", listOf("Attempt one paper under timed conditions.")),
                    TaskGroup("Analysis", listOf("Review conceptual and application-based mistakes.", "Revise three recurring weak areas."))
                )
            ),
            AcademicDay(
                id = "tifr_6",
                phaseId = "tifr",
                dateLabel = "Sat · 31 Oct",
                title = "TIFR Paper 6",
                tag = "Reattempt",
                groups = listOf(
                    TaskGroup("Timed paper", listOf("Attempt one paper under timed conditions.")),
                    TaskGroup("Analysis", listOf("Reattempt selected questions you previously got wrong.", "Review mechanisms and experimental concepts."))
                )
            ),
            AcademicDay(
                id = "tifr_7",
                phaseId = "tifr",
                dateLabel = "Sun · 1 Nov",
                title = "TIFR Paper 7 + Review",
                tag = "Weekly review",
                groups = listOf(
                    TaskGroup("Practice", listOf("Attempt another official paper; if unavailable, use a legitimate practice set.")),
                    TaskGroup("Review", listOf("List your ten weakest concepts.", "Choose the three subjects needing most attention.", "Prepare materials for core biology week."))
                )
            ),
            // Week 1
            AcademicDay(
                id = "w1_1",
                phaseId = "week1",
                dateLabel = "Mon · 2 Nov",
                title = "Carbohydrates and Lipids",
                tag = "Biomolecules",
                groups = listOf(
                    TaskGroup("PW lecture + reference", listOf("Watch PW lecture on carbohydrates and cover monosaccharides, disaccharides and polysaccharides.", "Study glycosidic bonds and reducing sugars.", "Study fatty acids, triglycerides, phospholipids and membrane lipids.")),
                    TaskGroup("Questions + recall", listOf("Solve 15–20 carbohydrate/lipid questions, including relevant JAM or CSIR-NET PYQs.", "Explain starch vs glycogen vs cellulose without notes.", "Explain why phospholipids form membranes."))
                )
            ),
            AcademicDay(
                id = "w1_2",
                phaseId = "week1",
                dateLabel = "Tue · 3 Nov",
                title = "Amino Acids and Proteins",
                tag = "PW Debashish Sir",
                groups = listOf(
                    TaskGroup("PW lecture + reference", listOf("Use PW Debashish Sir as the primary resource for protein biochemistry.", "Study amino acid classification, peptide bonds and four levels of protein structure.", "Review folding, denaturation and forces stabilising protein structure.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 protein/amino-acid questions.", "Draw a peptide bond and compare the four protein-structure levels.", "Explain how pH and temperature affect proteins."))
                )
            ),
            AcademicDay(
                id = "w1_3",
                phaseId = "week1",
                dateLabel = "Wed · 4 Nov",
                title = "Nucleic Acids and Nucleotides",
                tag = "Biomolecules",
                groups = listOf(
                    TaskGroup("PW lecture + reference", listOf("Study purines, pyrimidines, nucleosides and nucleotides.", "Review DNA vs RNA, phosphodiester bonds and complementary base pairing.", "Practise identifying 5′ and 3′ strand ends.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 nucleic-acid questions.", "Attempt relevant molecular-biology PYQs.", "Explain nucleoside vs nucleotide and why base pairing matters."))
                )
            ),
            AcademicDay(
                id = "w1_4",
                phaseId = "week1",
                dateLabel = "Thu · 5 Nov",
                title = "Enzyme Kinetics",
                tag = "Biochemistry",
                groups = listOf(
                    TaskGroup("PW/GDC + reference", listOf("Study Michaelis–Menten kinetics, Km and Vmax.", "Understand substrate concentration vs reaction velocity.", "Use GDC only if you need extra help with numerical or graph-based applications.")),
                    TaskGroup("Questions + recall", listOf("Solve 15–20 conceptual questions and 5–10 numericals.", "Interpret enzyme-kinetics graphs.", "Explain Km and solve one kinetics problem without notes."))
                )
            ),
            AcademicDay(
                id = "w1_5",
                phaseId = "week1",
                dateLabel = "Fri · 6 Nov",
                title = "Enzyme Inhibition & Regulation",
                tag = "Biochemistry",
                groups = listOf(
                    TaskGroup("Study", listOf("Compare competitive, non-competitive and uncompetitive inhibition.", "Study effects on Km and Vmax and Lineweaver–Burk plots.", "Review allosteric regulation and feedback inhibition.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 inhibition questions, including graph-based PYQs.", "Create a comparison table for the three inhibition types.", "Explain how feedback inhibition regulates a pathway."))
                )
            ),
            AcademicDay(
                id = "w1_6",
                phaseId = "week1",
                dateLabel = "Sat · 7 Nov",
                title = "Bioenergetics & Metabolism",
                tag = "Foundations",
                groups = listOf(
                    TaskGroup("Study", listOf("Review Gibbs free energy, exergonic vs endergonic reactions and ATP coupling.", "Study oxidation–reduction reactions and metabolic regulation.", "Begin glycolysis: purpose, energy investment and energy payoff.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 bioenergetics/metabolism questions.", "Practise ATP and redox questions.", "Draw a simplified overview of glycolysis."))
                )
            ),
            AcademicDay(
                id = "w1_7",
                phaseId = "week1",
                dateLabel = "Sun · 8 Nov",
                title = "Week 1 Consolidation",
                tag = "Test + review",
                groups = listOf(
                    TaskGroup("Revision + test", listOf("Revise biomolecules, enzyme kinetics, inhibition and bioenergetics.", "Take a 45–60 minute mixed test of about 30–40 questions.", "Review every incorrect or guessed answer.")),
                    TaskGroup("Weekly review", listOf("Update error log and list five weakest concepts.", "Make a 1–2 page revision sheet.", "Mark gaps that need GDC support."))
                )
            ),
            // Week 2
            AcademicDay(
                id = "w2_1",
                phaseId = "week2",
                dateLabel = "Mon · 9 Nov",
                title = "Membranes and Transport",
                tag = "Cell biology",
                groups = listOf(
                    TaskGroup("Study", listOf("Study membrane structure, lipids and proteins.", "Cover diffusion, facilitated diffusion, active transport, osmosis and permeability.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 membrane/transport questions.", "Compare passive and active transport.", "Explain the role of membrane proteins."))
                )
            ),
            AcademicDay(
                id = "w2_2",
                phaseId = "week2",
                dateLabel = "Tue · 10 Nov",
                title = "Organelles and Trafficking",
                tag = "Cell biology",
                groups = listOf(
                    TaskGroup("Study", listOf("Study nucleus, ER, Golgi, lysosomes and peroxisomes.", "Review mitochondria, chloroplasts and basic protein targeting.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 organelle questions.", "Trace the path of a secreted protein.", "Compare mitochondria and chloroplasts."))
                )
            ),
            AcademicDay(
                id = "w2_3",
                phaseId = "week2",
                dateLabel = "Wed · 11 Nov",
                title = "Cytoskeleton & Cell Division",
                tag = "Cell biology",
                groups = listOf(
                    TaskGroup("Study", listOf("Study microtubules, microfilaments and intermediate filaments.", "Review cell cycle, mitosis, meiosis and checkpoints.", "Distinguish apoptosis from necrosis at a basic level.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 questions.", "Draw the cell-cycle phases.", "Compare mitosis and meiosis."))
                )
            ),
            AcademicDay(
                id = "w2_4",
                phaseId = "week2",
                dateLabel = "Thu · 12 Nov",
                title = "DNA Replication",
                tag = "Molecular biology",
                groups = listOf(
                    TaskGroup("Study", listOf("Study semiconservative replication and DNA polymerases.", "Cover leading/lagging strands, Okazaki fragments, helicase, primase, ligase and topoisomerases.", "Compare prokaryotic and eukaryotic replication.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 DNA-replication questions.", "Draw a replication fork.", "Match replication enzymes with their functions."))
                )
            ),
            AcademicDay(
                id = "w2_5",
                phaseId = "week2",
                dateLabel = "Fri · 13 Nov",
                title = "Transcription & RNA Processing",
                tag = "Molecular biology",
                groups = listOf(
                    TaskGroup("Study", listOf("Study RNA polymerase, promoters and transcription stages.", "Compare prokaryotic and eukaryotic transcription.", "Cover 5′ capping, polyadenylation, introns, exons and splicing.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 transcription/RNA-processing questions.", "Attempt relevant PYQs.", "Explain the importance of RNA processing."))
                )
            ),
            AcademicDay(
                id = "w2_6",
                phaseId = "week2",
                dateLabel = "Sat · 14 Nov",
                title = "Translation & Gene Regulation",
                tag = "Molecular biology",
                groups = listOf(
                    TaskGroup("Study", listOf("Study genetic code, ribosome and translation stages.", "Study lac operon, transcription factors and regulatory proteins.", "Review basic chromatin and epigenetic regulation.")),
                    TaskGroup("Questions + recall", listOf("Solve 20–25 questions.", "Practise genetic-code questions and relevant PYQs.", "Explain the lac operon and distinguish transcriptional vs translational regulation."))
                )
            ),
            AcademicDay(
                id = "w2_7",
                phaseId = "week2",
                dateLabel = "Sun · 15 Nov",
                title = "Methods + Week 2 Test",
                tag = "Test + review",
                groups = listOf(
                    TaskGroup("Study", listOf("Review electrophoresis, PCR and Southern/Northern/Western blotting.", "Review microscopy, DNA sequencing principles and experimental-result interpretation.")),
                    TaskGroup("Test + review", listOf("Take a 45–60 minute mixed test of about 30–40 questions.", "Review incorrect and guessed answers.", "Make separate short revision sheets for cell biology and molecular biology.", "List topics requiring GDC support."))
                )
            )
        )
    }
}
