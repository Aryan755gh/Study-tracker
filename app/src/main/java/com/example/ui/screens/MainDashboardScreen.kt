package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.CalendarSyncManager
import com.example.ui.components.AudioPlayerBar
import com.example.ui.theme.*
import com.example.viewmodel.ScreenTab
import com.example.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity

    // Firebase Auth State
    val currentUser by viewModel.authManager.currentUser.collectAsState()
    val isAuthLoading by viewModel.authManager.isAuthLoading.collectAsState()
    val authError by viewModel.authManager.authError.collectAsState()

    // Pomodoro Timer State
    val totalSeconds by viewModel.timerDurationTotal.collectAsState()
    val secondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val isBreak by viewModel.isBreakMode.collectAsState()
    val timerCaption by viewModel.timerCaption.collectAsState()
    val isShieldActive by viewModel.isLockModeActive.collectAsState()

    // Gamification & Data
    val gamification by viewModel.repository.gamification.collectAsState()
    val academicDays by viewModel.repository.academicDays.collectAsState()
    val completedIds by viewModel.repository.completedTaskIds.collectAsState()

    var selectedSessionDuration by remember { mutableIntStateOf(25) }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val formattedTime = remember(secondsLeft) { String.format("%02d:%02d", minutes, seconds) }
    val progress = remember(secondsLeft, totalSeconds) {
        if (totalSeconds > 0) (secondsLeft.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
    }

    // Today's syllabus day (e.g. TIFR Day 1)
    val todayAcademicDay = remember(academicDays) {
        academicDays.firstOrNull() ?: academicDays.getOrNull(0)
    }

    val todayFormatted = remember {
        SimpleDateFormat("EEE · d MMM", Locale.getDefault()).format(Date()).uppercase()
    }

    // Infinite pulse animation for Start Button and Timer Ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val heroGlow by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroGlow"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudyBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Command Center Header & Firebase Sign-In Badge
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMMAND CENTRE",
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
                    }

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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Firebase Google Account Status Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudyPanel),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentUser != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(StudyPillBackground, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser?.displayName?.take(1) ?: "U",
                                        color = StudyMint,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentUser?.displayName ?: "Google Scholar",
                                        color = StudyTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Cloud Synced",
                                            tint = StudyMint,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Firebase Cloud Synced",
                                            color = StudyMint,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            TextButton(
                                onClick = { viewModel.authManager.signOut(coroutineScope) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Sign out", color = StudyTextMuted, fontSize = 11.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = "Cloud Offline",
                                    tint = StudyTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sync study stats to Cloud",
                                    color = StudyTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (activity != null) {
                                        viewModel.authManager.signInWithGoogle(activity, coroutineScope)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudyBlue,
                                    contentColor = Color(0xFF0D254C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isAuthLoading,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("google_signin_button")
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFF0D254C),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Sign in with Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // HERO: 'START STUDY SESSION' Button & Duration Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(if (isRunning) 1f else heroGlow)
                    .testTag("start_study_session_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudyPanel),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isRunning) StudyYellow else StudyMint
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isRunning) Color(0xFF2E2413) else Color(0xFF132D23),
                                    StudyPanel
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                shape = CircleShape,
                                color = if (isRunning) Color(0xFF453412) else StudyPillBackground,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Default.Bolt else Icons.Default.Security,
                                        contentDescription = "Shield Status",
                                        tint = if (isRunning) StudyYellow else StudyMint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRunning) "SESSION IN PROGRESS" else "DEEP WORK READY",
                                        color = if (isRunning) StudyYellow else StudyMint,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = if (isRunning) "Deep Flow Active" else "Ready to Focus?",
                                color = StudyTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (isRunning) "Distraction shield active · Binaural audio running"
                                else "Locks distractions, starts binaural waves & tracks progress",
                                color = StudyTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Status Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isRunning) StudyYellow.copy(alpha = 0.2f) else StudyMint.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Psychology else Icons.Default.PlayArrow,
                                contentDescription = "Focus",
                                tint = if (isRunning) StudyYellow else StudyMint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isRunning) {
                        // Duration Preset Selector
                        Text(
                            text = "CHOOSE SESSION DURATION:",
                            color = StudyTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(15, 25, 50, 90).forEach { mins ->
                                val isSelected = selectedSessionDuration == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSessionDuration = mins },
                                    label = { Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = StudyPanelElevated,
                                        selectedContainerColor = StudyMint,
                                        labelColor = StudyTextPrimary,
                                        selectedLabelColor = Color(0xFF092017)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = StudyBorder,
                                        selectedBorderColor = StudyMint
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large 'Start Study Session' Action Button
                        Button(
                            onClick = {
                                viewModel.startStudySession(selectedSessionDuration)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_study_session_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudyMint,
                                contentColor = Color(0xFF092017)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START STUDY SESSION ($selectedSessionDuration MIN)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        // When Running: Quick Pause / Extend / Open Shield
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.pauseTimer() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudyYellow,
                                    contentColor = Color(0xFF3A2800)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.setTab(ScreenTab.STUDY_MODE) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudyMint,
                                    contentColor = Color(0xFF092017)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = "Study Hub", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Study Hub / Channels", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // INTEGRATED POMODORO TIMER WIDGET (Directly on Dashboard)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_pomodoro_widget"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = StudyMint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pomodoro Focus Timer",
                                color = StudyTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isBreak) StudySecondaryContainer else StudyPillBackground
                        ) {
                            Text(
                                text = if (isBreak) "BREAK" else if (isRunning) "FOCUSING" else "IDLE",
                                color = if (isBreak) StudyBlue else StudyMint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Compact Circular Arc Display
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 8.dp.toPx()
                                drawArc(
                                    color = StudyPanelElevated,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke)
                                )
                                drawArc(
                                    color = if (isBreak) StudyBlue else StudyMint,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formattedTime,
                                    color = StudyTextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isBreak) "Break" else "Focus",
                                    color = StudyTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Timer Quick Controls
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = timerCaption,
                                color = StudyTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRunning) StudyYellow else StudyMint,
                                        contentColor = if (isRunning) Color(0xFF3A2800) else Color(0xFF092017)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isRunning) "Pause" else "Start",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isRunning) "Pause" else "Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.resetTimer() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(StudyPanelElevated, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset",
                                        tint = StudyTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // QUICK-VIEW: TODAY'S CALENDAR EVENTS & SYLLABUS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_calendar_quickview"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = StudyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's Academic Calendar",
                                color = StudyTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { viewModel.setTab(ScreenTab.CALENDAR) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Full Plan →", color = StudyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (todayAcademicDay != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = StudyPanelElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = todayAcademicDay.dateLabel,
                                        color = StudyTextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = todayAcademicDay.title,
                                        color = StudyTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = StudySurfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = todayAcademicDay.tag,
                                        color = StudyBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scheduled task breakdown
                        Text(
                            text = "TODAY'S SCHEDULED SYLLABUS BLOCKS:",
                            color = StudyTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        todayAcademicDay.groups.forEach { group ->
                            Text(
                                text = group.groupTitle,
                                color = StudyMint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )

                            group.tasks.forEach { taskText ->
                                val taskId = "${todayAcademicDay.id}_${group.groupTitle}_${taskText.take(15)}".replace(" ", "_")
                                val isDone = completedIds.contains(taskId)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.repository.toggleTaskCompletion(taskId) }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = { viewModel.repository.toggleTaskCompletion(taskId) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = StudyMint,
                                            uncheckedColor = StudyBorderLight,
                                            checkmarkColor = Color(0xFF092017)
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = taskText,
                                        color = if (isDone) StudyTextMuted else StudyTextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // One-Tap Add to Google Calendar with 15m & 30m reminders
                        OutlinedButton(
                            onClick = {
                                viewModel.syncAcademicDayToGoogleCalendar(context, todayAcademicDay)
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
                                contentDescription = "Google Calendar Sync",
                                tint = StudyBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sync Today's Event to Google Calendar (15m Alert)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Ambient Audio Synthesizer Bar
        item {
            AudioPlayerBar(viewModel = viewModel)
        }

        // Daily Progress & Streak Triad
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus progress
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = StudyPanel),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TODAY", color = StudyTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${gamification.minutesStudiedToday}", color = StudyTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text(" / ${gamification.targetMinutes}m", color = StudyTextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        val prog = (gamification.minutesStudiedToday.toFloat() / gamification.targetMinutes).coerceIn(0f, 1f)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = StudyMint, trackColor = StudySurfaceVariant)
                    }
                }

                // Streak
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = StudyPanel),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("STREAK", color = StudyTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${gamification.streak}", color = StudyYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(Icons.Default.Whatshot, contentDescription = "Streak", tint = StudyYellow, modifier = Modifier.size(16.dp))
                        }
                        Text("days consistent", color = StudyTextSecondary, fontSize = 10.sp)
                    }
                }

                // Scholar Rank
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = StudyPanel),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("RANK", color = StudyTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("Lv.${gamification.level}", color = StudyMint, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("${gamification.xp} XP", color = StudyTextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
