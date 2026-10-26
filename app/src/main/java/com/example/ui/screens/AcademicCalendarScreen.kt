package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.CalendarSyncManager
import com.example.model.AcademicDay
import com.example.ui.theme.*
import com.example.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicCalendarScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val academicDays by viewModel.repository.academicDays.collectAsState()
    val completedIds by viewModel.repository.completedTaskIds.collectAsState()
    val selectedFilter by viewModel.selectedPhaseFilter.collectAsState()

    var hasCalendarPermission by remember {
        mutableStateOf(CalendarSyncManager.hasCalendarPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCalendarPermission = perms[android.Manifest.permission.READ_CALENDAR] == true &&
                perms[android.Manifest.permission.WRITE_CALENDAR] == true
        if (hasCalendarPermission) {
            Toast.makeText(context, "Google Calendar permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredDays = remember(academicDays, selectedFilter) {
        if (selectedFilter == "all") academicDays
        else academicDays.filter { it.phaseId == selectedFilter }
    }

    // Calculate total and done across syllabus
    val totalTasks = academicDays.sumOf { d -> d.groups.sumOf { g -> g.tasks.size } }
    val doneTasks = academicDays.sumOf { d ->
        d.groups.sumOf { g ->
            g.tasks.count { t ->
                val id = "${d.id}_${g.groupTitle}_${t.take(15)}".replace(" ", "_")
                completedIds.contains(id)
            }
        }
    }
    val overallPct = if (totalTasks > 0) (doneTasks.toFloat() / totalTasks * 100).toInt() else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudyBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ACADEMIC TIMELINE & SYLLABUS",
                    color = StudyMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Academic Calendar",
                    color = StudyTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Structured phases synced with Google Calendar for daily reminders",
                    color = StudyTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Google Calendar Sync Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_calendar_sync_card"),
                colors = CardDefaults.cardColors(containerColor = StudyPanel),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StudyBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Google Calendar Sync",
                                    tint = StudyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Calendar Integration",
                                color = StudyTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasCalendarPermission) "Connected · Auto 15m & 30m alerts" else "Tap to connect Google Calendar",
                                color = if (hasCalendarPermission) StudyMint else StudyTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!hasCalendarPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.READ_CALENDAR,
                                            android.Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                } else {
                                    viewModel.syncAllSyllabusToGoogleCalendar(context)
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("sync_google_calendar_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudyBlue,
                                contentColor = Color(0xFF0D254C)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasCalendarPermission) "Sync Syllabus" else "Connect Calendar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val ics = CalendarSyncManager.generateIcsExport(academicDays)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("StudyOS iCalendar (.ics)", ics)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "iCalendar (.ics) copied! Import into Google Calendar.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(StudyBorderLight)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudyTextPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy .ics",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export .ICS", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Overall Syllabus Progress Card
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
                            text = "Overall Syllabus Progress",
                            color = StudyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$overallPct%",
                            color = StudyMint,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$doneTasks of $totalTasks study topics completed",
                        color = StudyTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { overallPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = StudyMint,
                        trackColor = StudySurfaceVariant
                    )
                }
            }
        }

        // Phase Filter Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CHOOSE PHASE:",
                    color = StudyTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val filterOptions = listOf(
                    "all" to "All Tasks",
                    "tifr" to "TIFR Paper Week (26 Oct–1 Nov)",
                    "week1" to "Week 1 · Biomolecules (2–8 Nov)",
                    "week2" to "Week 2 · Cell & Molecular (9–15 Nov)"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filterOptions.forEach { (id, label) ->
                        val isSelected = selectedFilter == id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPhaseFilter(id) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudyPanel,
                                selectedContainerColor = StudyPillBackground,
                                labelColor = StudyTextPrimary,
                                selectedLabelColor = StudyMint
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
            }
        }

        // Daily Syllabus Items
        items(filteredDays, key = { it.id }) { day ->
            AcademicDayCard(
                day = day,
                completedIds = completedIds,
                onToggleTask = { taskId ->
                    viewModel.repository.toggleTaskCompletion(taskId)
                },
                onAddToCalendar = {
                    viewModel.syncAcademicDayToGoogleCalendar(context, day)
                }
            )
        }
    }
}

@Composable
fun AcademicDayCard(
    day: AcademicDay,
    completedIds: Set<String>,
    onToggleTask: (String) -> Unit,
    onAddToCalendar: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StudyPanel),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = day.dateLabel,
                        color = StudyTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = day.title,
                        color = StudyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StudySurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = day.tag,
                            color = StudyBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onAddToCalendar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Add to Google Calendar",
                            tint = StudyMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = StudyTextMuted
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    day.groups.forEach { group ->
                        Text(
                            text = group.groupTitle,
                            color = StudyMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        group.tasks.forEach { taskText ->
                            val taskId = "${day.id}_${group.groupTitle}_${taskText.take(15)}".replace(" ", "_")
                            val isDone = completedIds.contains(taskId)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onToggleTask(taskId) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = isDone,
                                    onCheckedChange = { onToggleTask(taskId) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = StudyMint,
                                        uncheckedColor = StudyBorderLight,
                                        checkmarkColor = Color(0xFF092017)
                                    ),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = taskText,
                                    color = if (isDone) StudyTextMuted else StudyTextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
