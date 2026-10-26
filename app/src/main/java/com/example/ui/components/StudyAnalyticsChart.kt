package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DayProgressPoint
import com.example.ui.theme.*

enum class ChartMetricTab {
    FOCUS_TIME,
    STREAK_HEATMAP,
    ACCURACY_PYQ
}

@Composable
fun StudyAnalyticsChart(
    progressPoints: List<DayProgressPoint>,
    streakDays: Int,
    targetMinutes: Int,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ChartMetricTab.FOCUS_TIME) }
    var selectedPointIndex by remember { mutableIntStateOf(progressPoints.lastIndex.coerceAtLeast(0)) }

    // Keep selectedPoint updated when progressPoints change
    val activePoint = progressPoints.getOrNull(selectedPointIndex) ?: progressPoints.lastOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_analytics_chart_card"),
        colors = CardDefaults.cardColors(containerColor = StudyPanel),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title & Room SQLite indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(StudyPillBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Analytics",
                            tint = StudyMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ROOM DB TELEMETRY",
                            color = StudyMint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Study Progress & Streaks",
                            color = StudyTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudySurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorderLight)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(StudyMint, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Room SQLite",
                            color = StudyTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recharts-inspired Metric Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudyPanelElevated, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartMetricTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val label = when (tab) {
                        ChartMetricTab.FOCUS_TIME -> "Focus Time"
                        ChartMetricTab.STREAK_HEATMAP -> "Streak Matrix"
                        ChartMetricTab.ACCURACY_PYQ -> "PYQ Accuracy"
                    }
                    val icon = when (tab) {
                        ChartMetricTab.FOCUS_TIME -> Icons.Default.AccessTime
                        ChartMetricTab.STREAK_HEATMAP -> Icons.Default.Whatshot
                        ChartMetricTab.ACCURACY_PYQ -> Icons.Default.FactCheck
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StudySurfaceVariant else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) StudyMint else StudyTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = if (isSelected) StudyTextPrimary else StudyTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visualizations per Selected Tab
            when (selectedTab) {
                ChartMetricTab.FOCUS_TIME -> {
                    FocusTimeBarChart(
                        points = progressPoints.takeLast(7),
                        targetMinutes = targetMinutes,
                        selectedIndex = selectedPointIndex % 7,
                        onSelectIndex = { selectedPointIndex = it }
                    )
                }
                ChartMetricTab.STREAK_HEATMAP -> {
                    StreakHeatmapMatrix(
                        points = progressPoints,
                        streakDays = streakDays,
                        selectedIndex = selectedPointIndex,
                        onSelectIndex = { selectedPointIndex = it }
                    )
                }
                ChartMetricTab.ACCURACY_PYQ -> {
                    AccuracyPyqChart(
                        points = progressPoints.takeLast(7),
                        selectedIndex = selectedPointIndex % 7,
                        onSelectIndex = { selectedPointIndex = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Recharts Tooltip / Detail Card
            if (activePoint != null) {
                Surface(
                    color = StudyPanelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${activePoint.dayName} · ${activePoint.shortDate}",
                                    color = StudyTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (activePoint.focusMinutes >= activePoint.targetMinutes) {
                                    Surface(
                                        shape = CircleShape,
                                        color = StudyPillBackground
                                    ) {
                                        Text(
                                            text = "GOAL MET",
                                            color = StudyMint,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${activePoint.focusMinutes} min",
                                color = if (activePoint.focusMinutes >= activePoint.targetMinutes) StudyMint else StudyYellow,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricDetailItem(
                                label = "Sessions in Room",
                                value = "${activePoint.sessionCount} logged",
                                icon = Icons.Default.Dataset
                            )
                            MetricDetailItem(
                                label = "Questions Attempted",
                                value = "${activePoint.questionsSolved} Qs",
                                icon = Icons.Default.Quiz
                            )
                            MetricDetailItem(
                                label = "Accuracy",
                                value = "${activePoint.accuracy}%",
                                icon = Icons.Default.GpsFixed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Database sync status & refresh telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Room Storage",
                        tint = StudyTextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "14 days cached in study_os_room.db",
                        color = StudyTextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh telemetry",
                        tint = StudyMint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricDetailItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column {
        Text(
            text = label.uppercase(),
            color = StudyTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StudyBlue,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                color = StudyTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------
// 1. Recharts-style Focus Time Bar Chart with Reference Target Line
// -------------------------------------------------------------------------
@Composable
fun FocusTimeBarChart(
    points: List<DayProgressPoint>,
    targetMinutes: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val maxMinutes = remember(points, targetMinutes) {
        maxOf(targetMinutes * 1.25f, (points.maxOfOrNull { it.focusMinutes } ?: 240).toFloat())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAILY FOCUS DURATION (MINUTES)",
                color = StudyTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp, 2.dp)
                        .background(StudyYellow)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Goal ${targetMinutes}m",
                    color = StudyYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Canvas Chart (Recharts style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(StudyPanelElevated, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / points.size.coerceAtLeast(1)
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val count = points.size.coerceAtLeast(1)
                val slotWidth = width / count
                val barWidth = (slotWidth * 0.52f).coerceAtMost(32.dp.toPx())

                // Draw horizontal guide lines (0m, 120m, 240m)
                val targetY = height - ((targetMinutes / maxMinutes) * height)
                val midY = height - (((targetMinutes / 2f) / maxMinutes) * height)

                // Mid grid line
                drawLine(
                    color = Color(0xFF23354E),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.dp.toPx()
                )

                // Target Guideline (dashed)
                drawLine(
                    color = Color(0xFFFFD27D).copy(alpha = 0.7f),
                    start = Offset(0f, targetY),
                    end = Offset(width, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Draw Bars
                points.forEachIndexed { index, point ->
                    val isSelected = index == selectedIndex
                    val barHeight = ((point.focusMinutes / maxMinutes) * height).coerceIn(4.dp.toPx(), height)
                    val barLeft = (index * slotWidth) + (slotWidth - barWidth) / 2f
                    val barTop = height - barHeight

                    val gradientBrush = if (point.focusMinutes >= targetMinutes) {
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFD27D), Color(0xFF7CDBB1)),
                            startY = barTop,
                            endY = height
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFF7CDBB1), Color(0xFF8BB8FF)),
                            startY = barTop,
                            endY = height
                        )
                    }

                    // Bar body
                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // If selected, highlight border
                    if (isSelected) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(barLeft - 2.dp.toPx(), barTop - 2.dp.toPx()),
                            size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis Day Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectIndex(index) }
                ) {
                    Text(
                        text = point.dayName.take(1),
                        color = if (isSelected) StudyMint else StudyTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                    Text(
                        text = point.shortDate.split(" ").firstOrNull() ?: "",
                        color = if (isSelected) StudyTextPrimary else StudyTextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 2. D3-style Streak Activity Heatmap Grid
// -------------------------------------------------------------------------
@Composable
fun StreakHeatmapMatrix(
    points: List<DayProgressPoint>,
    streakDays: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "14-DAY STREAK CONSISTENCY MATRIX",
                color = StudyTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Streak",
                    tint = StudyYellow,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$streakDays Days Active",
                    color = StudyYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Heatmap Cells Grid (2 rows of 7 days)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudyPanelElevated, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chunked = points.takeLast(14).chunked(7)
            chunked.forEachIndexed { rowIndex, weekRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekRow.forEachIndexed { colIndex, pt ->
                        val globalIdx = rowIndex * 7 + colIndex
                        val isSelected = globalIdx == selectedIndex

                        val cellColor = when (pt.streakLevel) {
                            4 -> Color(0xFF7CDBB1) // >240m full mint
                            3 -> Color(0xFF5ABF93) // >180m
                            2 -> Color(0xFF388567) // >120m
                            1 -> Color(0xFF204E3E) // >0m
                            else -> Color(0xFF162338) // 0m
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectIndex(globalIdx) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(cellColor, RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier.border(1.dp, StudyBorder, RoundedCornerShape(8.dp))
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pt.focusMinutes >= pt.targetMinutes) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete",
                                        tint = if (pt.streakLevel >= 3) Color(0xFF062319) else StudyMint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (pt.streakLevel > 0) {
                                    Text(
                                        text = "${pt.focusMinutes / 60}h",
                                        color = if (pt.streakLevel >= 3) Color(0xFF062319) else StudyTextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pt.dayName.take(1),
                                color = if (isSelected) StudyMint else StudyTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Heatmap Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", color = StudyTextMuted, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(4.dp))
            listOf(
                Color(0xFF162338),
                Color(0xFF204E3E),
                Color(0xFF388567),
                Color(0xFF5ABF93),
                Color(0xFF7CDBB1)
            ).forEach { col ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(10.dp)
                        .background(col, RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Goal (4h+)", color = StudyTextMuted, fontSize = 9.sp)
        }
    }
}

// -------------------------------------------------------------------------
// 3. Recharts-style Accuracy & PYQ Practice Progress
// -------------------------------------------------------------------------
@Composable
fun AccuracyPyqChart(
    points: List<DayProgressPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PYQ VOLUME & ACCURACY RATE",
                color = StudyTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Target ≥ 85% Acc",
                color = StudyBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudyPanelElevated, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            points.forEachIndexed { index, pt ->
                val isSelected = index == selectedIndex
                val accuracyFraction = (pt.accuracy / 100f).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectIndex(index) }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pt.dayName} · ${pt.shortDate}",
                        color = if (isSelected) StudyMint else StudyTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.width(70.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .background(StudySurfaceVariant, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(accuracyFraction)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(StudyBlue, StudyMint)
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "${pt.accuracy}%",
                        color = if (pt.accuracy >= 85) StudyMint else StudyYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )

                    Text(
                        text = "(${pt.questionsSolved} Qs)",
                        color = StudyTextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
