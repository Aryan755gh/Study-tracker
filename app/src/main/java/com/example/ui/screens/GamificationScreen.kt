package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudyBadge
import com.example.ui.theme.*
import com.example.viewmodel.StudyViewModel

@Composable
fun GamificationScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val gamification by viewModel.repository.gamification.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudyBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "GAMIFIED ACADEMIC MASTERY",
                    color = StudyMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Scholar Rank & Stats",
                    color = StudyTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Level up by completing deep sessions, syllabus tasks, and logs",
                    color = StudyTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Scholar Rank & XP Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scholar_rank_card"),
                colors = CardDefaults.cardColors(containerColor = StudyPanel),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = StudyPillBackground,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Lv.${gamification.level}",
                                        color = StudyMint,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = gamification.rankTitle,
                                    color = StudyTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${gamification.xp} Total XP",
                                    color = StudyYellow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = StudySurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Streak",
                                    tint = StudyYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${gamification.streak} Days",
                                    color = StudyTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level ${gamification.level} Progress",
                            color = StudyTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${gamification.xp} / ${gamification.xpForNextLevel} XP",
                            color = StudyMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { gamification.levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = StudyMint,
                        trackColor = StudySurfaceVariant
                    )
                }
            }
        }

        // Metrics Triad
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "FOCUS TIME",
                    value = String.format("%.1f", gamification.totalFocusHours),
                    unit = " hrs",
                    subtitle = "Deep flow",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "QUESTIONS",
                    value = "${gamification.questionsSolvedTotal}",
                    unit = "",
                    subtitle = "Solved & logged",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "STREAK",
                    value = "${gamification.streak}",
                    unit = " d",
                    subtitle = "Active streak",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 30-Day Activity Heatmap
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
                            text = "30-Day Consistency Grid",
                            color = StudyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Timeline",
                            tint = StudyMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5 rows x 6 columns grid of 30 days
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (row in 0 until 5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                for (col in 0 until 6) {
                                    val index = row * 6 + col
                                    // Generate nice realistic activity levels for preview
                                    val level = when {
                                        index in 26..29 -> 3 // high recent activity
                                        index in listOf(1, 4, 7, 8, 12, 15, 18, 20, 22, 24) -> 2
                                        index in listOf(2, 6, 11, 16, 21) -> 1
                                        else -> 0
                                    }
                                    val color = when (level) {
                                        3 -> StudyMint
                                        2 -> StudyMint.copy(alpha = 0.6f)
                                        1 -> StudyMint.copy(alpha = 0.3f)
                                        else -> StudySurfaceVariant
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Less",
                            color = StudyTextMuted,
                            fontSize = 10.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(StudySurfaceVariant, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(10.dp).background(StudyMint.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(10.dp).background(StudyMint.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(10.dp).background(StudyMint, RoundedCornerShape(2.dp)))
                        }
                        Text(
                            text = "More",
                            color = StudyTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Academic Badges Section
        item {
            Text(
                text = "Academic Badges",
                color = StudyTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(gamification.badges, key = { it.id }) { badge ->
            BadgeRowCard(badge = badge)
        }

        // XP Scoring System Card
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
                        text = "How to Earn XP",
                        color = StudyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val xpRules = listOf(
                        "+1 XP" to "Per minute of completed focus session",
                        "+25 XP" to "Per completed mission or syllabus task",
                        "+35 XP" to "For logging daily reflection & doubts",
                        "+50 XP" to "High question accuracy (>80%) on test",
                        "+100 XP" to "Sync academic syllabus to Google Calendar"
                    )

                    xpRules.forEach { (points, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = StudyPillBackground,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = points,
                                    color = StudyMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = desc,
                                color = StudyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = StudyPanel),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = StudyTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = StudyTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = unit,
                    color = StudyTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                text = subtitle,
                color = StudyTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun BadgeRowCard(badge: StudyBadge) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("badge_card_${badge.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) StudyPanel else StudyPanel.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (badge.isUnlocked) StudyMint.copy(alpha = 0.5f) else StudyBorder
            )
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (badge.isUnlocked) StudyPillBackground else StudySurfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.iconEmoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge.title,
                    color = if (badge.isUnlocked) StudyTextPrimary else StudyTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = badge.description,
                    color = StudyTextSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = if (badge.isUnlocked) StudyMint.copy(alpha = 0.2f) else StudySurfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (badge.isUnlocked) "UNLOCKED" else "LOCKED",
                    color = if (badge.isUnlocked) StudyMint else StudyTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
