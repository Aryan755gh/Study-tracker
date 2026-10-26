package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.CalendarSyncManager
import com.example.model.DailyStudyLog
import com.example.model.StudyTask
import com.example.ui.components.AudioPlayerBar
import com.example.ui.theme.*
import com.example.viewmodel.ScreenTab
import com.example.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayMissionScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.repository.todayTasks.collectAsState()
    val completedIds by viewModel.repository.completedTaskIds.collectAsState()
    val gamification by viewModel.repository.gamification.collectAsState()
    val todayLog by viewModel.repository.todayLog.collectAsState()
    val strategyRules by viewModel.repository.strategyRules.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskTag by remember { mutableStateOf("CORE") }
    var newTaskDuration by remember { mutableStateOf("45") }

    var logTopic by remember(todayLog.topic) { mutableStateOf(todayLog.topic) }
    var logQuestionsAtt by remember(todayLog.questionsAttempted) { mutableStateOf(if (todayLog.questionsAttempted > 0) todayLog.questionsAttempted.toString() else "") }
    var logQuestionsCorr by remember(todayLog.questionsCorrect) { mutableStateOf(if (todayLog.questionsCorrect > 0) todayLog.questionsCorrect.toString() else "") }
    var logNotes by remember(todayLog.notes) { mutableStateOf(todayLog.notes) }
    var saveStatusMsg by remember { mutableStateOf("") }

    val completedCount = tasks.count { completedIds.contains(it.id) }
    val taskPct = if (tasks.isNotEmpty()) (completedCount.toFloat() / tasks.size * 100).toInt() else 0

    val todayFormatted = remember {
        SimpleDateFormat("EEE · d MMM", Locale.getDefault()).format(Date()).uppercase()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudyBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR PERSONAL STUDY COMMAND CENTRE",
                            color = StudyMint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "StudyOS",
                                color = StudyTextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = ".",
                                color = StudyMint,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "Small sessions. Honest tracking. Steady progress.",
                            color = StudyTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = CircleShape,
                            color = StudyPillBackground,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(StudyPillBorder)
                            )
                        ) {
                            Text(
                                text = todayFormatted,
                                color = StudyMint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = CircleShape,
                            color = StudySurfaceVariant,
                            modifier = Modifier.clickable { viewModel.setTab(ScreenTab.GAMIFICATION) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Lv.${gamification.level}",
                                    color = StudyYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${gamification.xp} XP",
                                    color = StudyTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ambient Audio Banner
        item {
            AudioPlayerBar(viewModel = viewModel)
        }

        // Stats Triad Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus Time Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = StudyPanel),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "FOCUSED TODAY",
                            color = StudyTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${gamification.minutesStudiedToday}",
                                color = StudyTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " min",
                                color = StudyTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Text(
                            text = "Target: ${gamification.targetMinutes}m",
                            color = StudyTextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = (gamification.minutesStudiedToday.toFloat() / gamification.targetMinutes).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = StudyMint,
                            trackColor = StudySurfaceVariant
                        )
                    }
                }

                // Tasks Completed Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = StudyPanel),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TASKS DONE",
                            color = StudyTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$completedCount",
                                color = StudyTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " / ${tasks.size}",
                                color = StudyTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Text(
                            text = "$taskPct% completed",
                            color = StudyTextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { taskPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = StudyBlue,
                            trackColor = StudySurfaceVariant
                        )
                    }
                }

                // Streak Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = StudyPanel),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "STREAK",
                            color = StudyTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${gamification.streak}",
                                color = StudyYellow,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = "Streak flame",
                                tint = StudyYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "days in a row",
                            color = StudyTextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Consistency wins",
                            color = StudyMint,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Today's Mission Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_mission_card"),
                colors = CardDefaults.cardColors(containerColor = StudyPanel),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Mission",
                                color = StudyTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tick each task as you finish. Progress saves on this device.",
                                color = StudyTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { showAddTaskDialog = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add custom task",
                                    tint = StudyMint
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.repository.resetTodayTasks()
                                    Toast.makeText(context, "Today's tasks reset", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset day",
                                    tint = StudyTextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    tasks.forEach { task ->
                        val isDone = completedIds.contains(task.id)
                        TaskRowItem(
                            task = task,
                            isDone = isDone,
                            onToggle = { viewModel.repository.toggleTaskCompletion(task.id) },
                            onStartTimer = {
                                viewModel.setTimerPreset(task.durationMinutes)
                                viewModel.setTab(ScreenTab.POMODORO)
                                viewModel.startTimer()
                            }
                        )
                        HorizontalDivider(
                            color = StudyBorder.copy(alpha = 0.5f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Notice box
                    Surface(
                        color = Color(0xFF261E14),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF66512A))
                        )
                    ) {
                        Text(
                            text = "Reset day only clears today's ticks & session minutes. Your notes and saved history stay intact on this device.",
                            color = Color(0xFFFFDDA0),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Fixed Resource Strategy Card (From Attachment)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StudyPanel),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fixed Resource Strategy",
                        color = StudyTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Principles for high-yield biology & sciences preparation",
                        color = StudyTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val rules = listOf(
                        Triple("rule1", "PW Debashish Sir: primary for protein biochemistry and familiar foundational explanations.", "BIOCHEM"),
                        Triple("rule2", "GDC: use for broader integrated biochemistry, metabolism, quantitative applications and specific gaps.", "METABOLISM"),
                        Triple("rule3", "Don't watch two full courses for the same topic. Do questions first, then use second course only to fix a real gap.", "EFFICIENCY"),
                        Triple("rule4", "For every wrong/guessed question, label it K (knowledge), R (recall), A (application), D (data interpretation), or S (strategy).", "ANALYSIS")
                    )

                    rules.forEach { (key, desc, tag) ->
                        val isChecked = strategyRules[key] ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.repository.toggleStrategyRule(key) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { viewModel.repository.toggleStrategyRule(key) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = StudyMint,
                                    uncheckedColor = StudyBorderLight,
                                    checkmarkColor = Color(0xFF092017)
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = desc,
                                    color = if (isChecked) StudyTextMuted else StudyTextPrimary,
                                    fontSize = 13.sp,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tag,
                                    color = StudyBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weekly Consistency Heatmap
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StudyPanel),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Consistency",
                            color = StudyTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Last 7 Days",
                            color = StudyTextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        days.forEachIndexed { i, dayName ->
                            val isToday = i == 0 // current day preview
                            val isActive = i in 0..2 // active streak mock/saved state
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isToday) StudyPillBackground else StudyPanelElevated
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isToday) StudyMint else StudyBorder
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayName,
                                        color = StudyTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${26 + i}",
                                        color = StudyTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isActive) "●" else "·",
                                        color = if (isActive) StudyMint else StudyTextMuted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A day counts when you complete at least one task or log a focus session.",
                        color = StudyTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Daily Study Log & Report Generator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_log_section"),
                colors = CardDefaults.cardColors(containerColor = StudyPanel),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Study Log & Reflection",
                            color = StudyTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = StudySurfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SAVED LOCALLY",
                                color = StudyBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = logTopic,
                        onValueChange = { logTopic = it },
                        label = { Text("Topic studied", color = StudyTextMuted) },
                        placeholder = { Text("e.g. Enzyme kinetics, Lineweaver–Burk plots", color = StudyTextMuted.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_topic_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudyBlue,
                            unfocusedBorderColor = StudyBorder,
                            focusedTextColor = StudyTextPrimary,
                            unfocusedTextColor = StudyTextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = logQuestionsAtt,
                            onValueChange = { logQuestionsAtt = it },
                            label = { Text("Questions Attempted", color = StudyTextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_questions_att_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudyBlue,
                                unfocusedBorderColor = StudyBorder,
                                focusedTextColor = StudyTextPrimary,
                                unfocusedTextColor = StudyTextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = logQuestionsCorr,
                            onValueChange = { logQuestionsCorr = it },
                            label = { Text("Correct", color = StudyTextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_questions_corr_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudyBlue,
                                unfocusedBorderColor = StudyBorder,
                                focusedTextColor = StudyTextPrimary,
                                unfocusedTextColor = StudyTextPrimary
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = logNotes,
                        onValueChange = { logNotes = it },
                        label = { Text("Mistakes, doubts & tomorrow's first task", color = StudyTextMuted) },
                        placeholder = { Text("What was difficult? (Label K/R/A/D/S) What will you start with tomorrow?", color = StudyTextMuted.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("log_notes_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudyBlue,
                            unfocusedBorderColor = StudyBorder,
                            focusedTextColor = StudyTextPrimary,
                            unfocusedTextColor = StudyTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val att = logQuestionsAtt.toIntOrNull() ?: 0
                                val corr = logQuestionsCorr.toIntOrNull() ?: 0
                                viewModel.repository.saveTodayLog(logTopic, att, corr, logNotes)
                                saveStatusMsg = "Saved for today (+35 XP!) ✓"
                                Toast.makeText(context, "Study Log Saved!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("save_log_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudyMint,
                                contentColor = Color(0xFF092017)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Today's Log", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val reportText = """
                                    Daily report — StudyOS
                                    Date: $todayFormatted
                                    Topic completed: $logTopic
                                    Study time: ${gamification.minutesStudiedToday} min
                                    Questions attempted: $logQuestionsAtt
                                    Questions correct: $logQuestionsCorr
                                    Main difficulties & doubts: $logNotes
                                    Status: Ready for next revision block
                                """.trimIndent()

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("StudyOS Daily Report", reportText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Report copied! Ready to paste into ChatGPT/notes.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_report_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(StudyBorderLight)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudyTextPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Report",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Report", fontSize = 12.sp)
                        }
                    }

                    if (saveStatusMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = saveStatusMsg,
                            color = StudyMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Google Calendar Quick Sync Button
                    OutlinedButton(
                        onClick = {
                            CalendarSyncManager.openAddEventIntent(
                                context = context,
                                title = if (logTopic.isNotEmpty()) logTopic else "Biology Study Session",
                                description = "StudyOS Session:\n$logNotes",
                                startMillis = System.currentTimeMillis() + 30 * 60 * 1000L,
                                durationMinutes = 120
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StudySurfaceVariant,
                            contentColor = StudyTextPrimary
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Sync to Google Calendar",
                            tint = StudyBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Today's Session to Google Calendar", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Add Custom Task Dialog
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Custom Mission Task", color = StudyTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskDuration,
                        onValueChange = { newTaskDuration = it },
                        label = { Text("Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CORE", "LEARN", "PRACTISE", "RECALL", "TEST").forEach { tag ->
                            FilterChip(
                                selected = newTaskTag == tag,
                                onClick = { newTaskTag = tag },
                                label = { Text(tag, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.repository.addCustomTask(
                                newTaskTitle,
                                newTaskTag,
                                newTaskDuration.toIntOrNull() ?: 45
                            )
                            showAddTaskDialog = false
                            newTaskTitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyMint, contentColor = Color(0xFF092017))
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = StudyTextSecondary)
                }
            },
            containerColor = StudyPanel
        )
    }
}

@Composable
fun TaskRowItem(
    task: StudyTask,
    isDone: Boolean,
    onToggle: () -> Unit,
    onStartTimer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = isDone,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = StudyMint,
                uncheckedColor = StudyBorderLight,
                checkmarkColor = Color(0xFF092017)
            ),
            modifier = Modifier
                .size(24.dp)
                .testTag("task_checkbox_${task.id}")
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = if (isDone) StudyTextMuted else StudyTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.meta.isNotEmpty()) {
                Text(
                    text = task.meta,
                    color = StudyTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StudyPanelElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    )
                ) {
                    Text(
                        text = "${task.durationMinutes} minutes",
                        color = StudyTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "▶ Focus",
                    color = StudyMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onStartTimer() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Surface(
            color = StudySurfaceVariant,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = task.tag,
                color = when (task.tag) {
                    "CORE" -> StudyMint
                    "LEARN" -> StudyBlue
                    "PRACTISE" -> StudyYellow
                    "TEST" -> StudyRed
                    else -> StudyPurple
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}
