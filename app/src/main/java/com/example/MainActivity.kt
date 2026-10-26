package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AcademicCalendarScreen
import com.example.ui.screens.GamificationScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.PomodoroScreen
import com.example.ui.screens.StudyModeScreen
import com.example.ui.screens.TodayMissionScreen
import com.example.ui.theme.*
import com.example.viewmodel.ScreenTab
import com.example.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {

    private val studyViewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyOSTheme {
                MainAppScaffold(viewModel = studyViewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: StudyViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    // BackHandler: If on any secondary tab, press back to return to DASHBOARD tab
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        viewModel.setTab(ScreenTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StudyBackground),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = StudyBackground,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = StudyPanel,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(ScreenTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                    Triple(ScreenTab.TODAY, Icons.Default.Today, "Missions"),
                    Triple(ScreenTab.CALENDAR, Icons.Default.CalendarMonth, "Calendar"),
                    Triple(ScreenTab.STUDY_MODE, Icons.Default.Security, "Study Mode"),
                    Triple(ScreenTab.GAMIFICATION, Icons.Default.EmojiEvents, "Scholar")
                )

                navItems.forEach { (tab, icon, label) ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF092017),
                            unselectedIconColor = StudyTextSecondary,
                            selectedTextColor = StudyMint,
                            unselectedTextColor = StudyTextMuted,
                            indicatorColor = StudyMint
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "ScreenTransition") { tab ->
                when (tab) {
                    ScreenTab.DASHBOARD -> MainDashboardScreen(viewModel = viewModel)
                    ScreenTab.TODAY -> TodayMissionScreen(viewModel = viewModel)
                    ScreenTab.CALENDAR -> AcademicCalendarScreen(viewModel = viewModel)
                    ScreenTab.POMODORO -> PomodoroScreen(viewModel = viewModel)
                    ScreenTab.STUDY_MODE -> StudyModeScreen(viewModel = viewModel)
                    ScreenTab.GAMIFICATION -> GamificationScreen(viewModel = viewModel)
                }
            }
        }
    }
}
