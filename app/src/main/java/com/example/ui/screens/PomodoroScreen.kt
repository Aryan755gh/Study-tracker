package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SoundType
import com.example.ui.components.AudioPlayerBar
import com.example.ui.theme.*
import com.example.viewmodel.ScreenTab
import com.example.viewmodel.StudyViewModel

@Composable
fun PomodoroScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val totalSeconds by viewModel.timerDurationTotal.collectAsState()
    val secondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val isBreak by viewModel.isBreakMode.collectAsState()
    val caption by viewModel.timerCaption.collectAsState()

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val formattedTime = remember(secondsLeft) {
        String.format("%02d:%02d", minutes, seconds)
    }

    val progress = remember(secondsLeft, totalSeconds) {
        if (totalSeconds > 0) (secondsLeft.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "timerRing")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title and Mode
        Text(
            text = "DEEP FOCUS ENGINE",
            color = StudyMint,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isBreak) StudyBlue.copy(alpha = 0.2f) else StudyPillBackground,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isBreak) StudyBlue else StudyMint
                    )
                )
            ) {
                Text(
                    text = if (isBreak) "BREAK SESSION" else "FOCUS SESSION",
                    color = if (isBreak) StudyBlue else StudyMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Circular Timer Display
        Box(
            modifier = Modifier
                .size(240.dp)
                .testTag("pomodoro_timer_circle"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()

                // Background track
                drawArc(
                    color = StudyPanelElevated,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                // Active progress arc
                drawArc(
                    color = if (isBreak) StudyBlue else StudyMint.copy(alpha = if (isRunning) glowPulse else 1f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedTime,
                    color = StudyTextPrimary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Text(
                    text = caption,
                    color = StudyTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer Controls: Play/Pause, Reset, Skip
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("timer_reset_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = StudyPanelElevated,
                    contentColor = StudyTextSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    modifier = Modifier.size(22.dp)
                )
            }

            Button(
                onClick = {
                    if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                },
                modifier = Modifier
                    .height(58.dp)
                    .widthIn(min = 140.dp)
                    .testTag("timer_start_pause_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) StudyYellow else StudyMint,
                    contentColor = if (isRunning) Color(0xFF3A2800) else Color(0xFF092017)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Pause" else "Start",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            FilledTonalIconButton(
                onClick = {
                    // Switch between break and study
                    viewModel.setTimerPreset(if (isBreak) 25 else 5, isBreak = !isBreak)
                },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("timer_skip_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = StudyPanelElevated,
                    contentColor = StudyTextSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Session",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preset Duration Chips
        Text(
            text = "SESSION PRESETS",
            color = StudyTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                Triple(15, false, "15m Sprint"),
                Triple(25, false, "25m Pomodoro"),
                Triple(50, false, "50m Deep Work"),
                Triple(5, true, "5m Break")
            )

            presets.forEach { (mins, isBrk, label) ->
                val isSelected = totalSeconds == mins * 60 && isBreak == isBrk
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setTimerPreset(mins, isBrk) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = StudyPanel,
                        selectedContainerColor = if (isBrk) StudySecondaryContainer else StudyPillBackground,
                        labelColor = StudyTextPrimary,
                        selectedLabelColor = if (isBrk) StudyBlue else StudyMint
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = StudyBorder,
                        selectedBorderColor = if (isBrk) StudyBlue else StudyMint
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Study Shield Shortcut Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudyPanel),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Study Shield",
                        tint = StudyMint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Distraction Shield & YouTube Study",
                            color = StudyTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Block shorts & study curated lectures",
                            color = StudyTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.setTab(ScreenTab.STUDY_MODE) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudySurfaceVariant,
                        contentColor = StudyMint
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ambient Audio Bar
        AudioPlayerBar(viewModel = viewModel)

        Spacer(modifier = Modifier.height(100.dp))
    }
}
