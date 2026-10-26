package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CuratedStudyChannel
import com.example.ui.components.AudioPlayerBar
import com.example.ui.theme.*
import com.example.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModeScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLockActive by viewModel.isLockModeActive.collectAsState()
    val lockDuration by viewModel.lockDurationMinutes.collectAsState()
    val secondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val channels by viewModel.repository.channels.collectAsState()
    val blockedItems by viewModel.repository.blockedDistractions.collectAsState()
    val activeChannel by viewModel.activeChannel.collectAsState()

    var showAddChannelDialog by remember { mutableStateOf(false) }
    var newChannelTitle by remember { mutableStateOf("") }
    var newChannelEducator by remember { mutableStateOf("") }
    var newChannelSubject by remember { mutableStateOf("") }
    var newChannelSearch by remember { mutableStateOf("") }

    var showEmergencyUnlockDialog by remember { mutableStateOf(false) }
    var inAppViewingUrl by remember { mutableStateOf<String?>(null) }
    var inAppViewingTitle by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "shieldPulse")
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldScale"
    )

    // In-App Educational Video Player (Blocks shorts, allows curated channel study)
    if (inAppViewingUrl != null) {
        EducationalWebStudyViewer(
            url = inAppViewingUrl!!,
            channelTitle = inAppViewingTitle,
            onClose = { inAppViewingUrl = null }
        )
        return
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
                Text(
                    text = "DISTRACTION BLOCKER & FOCUSED STUDY",
                    color = StudyMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Study Mode",
                    color = StudyTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Locks distracting shorts & social feeds while allowing approved study channels",
                    color = StudyTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Ambient Audio Banner
        item {
            AudioPlayerBar(viewModel = viewModel)
        }

        // Study Shield HUD Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_shield_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLockActive) Color(0xFF142B24) else StudyPanel
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isLockActive) StudyMint else StudyBorder
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(if (isLockActive) shieldScale else 1f)
                            .background(
                                if (isLockActive) StudyMint.copy(alpha = 0.2f) else StudySurfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLockActive) Icons.Default.Lock else Icons.Default.Security,
                            contentDescription = "Shield Status",
                            tint = if (isLockActive) StudyMint else StudyTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isLockActive) "STUDY SHIELD ACTIVE" else "STUDY SHIELD STANDBY",
                        color = if (isLockActive) StudyMint else StudyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = if (isLockActive)
                            "Distraction blocker engaged. Short-form feeds & social media are locked."
                        else
                            "Turn on Study Shield to lock out distractions and maintain deep focus.",
                        color = StudyTextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    if (isLockActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val mins = secondsLeft / 60
                        val secs = secondsLeft % 60
                        Surface(
                            color = StudyPillBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(StudyPillBorder)
                            )
                        ) {
                            Text(
                                text = "LOCK REMAINING: ${String.format("%02d:%02d", mins, secs)}",
                                color = StudyMint,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { showEmergencyUnlockDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudyRed),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(StudyRed.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency Unlock",
                                tint = StudyRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emergency Unlock (-50 XP Penalty)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Duration selectors
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(30, 45, 60, 90).forEach { mins ->
                                FilterChip(
                                    selected = lockDuration == mins,
                                    onClick = { viewModel.toggleStudyLockMode(mins) },
                                    label = { Text("${mins}m Lock", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = StudyPanelElevated,
                                        selectedContainerColor = StudyPillBackground,
                                        labelColor = StudyTextPrimary,
                                        selectedLabelColor = StudyMint
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = lockDuration == mins,
                                        borderColor = StudyBorder,
                                        selectedBorderColor = StudyMint
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.toggleStudyLockMode(lockDuration) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("activate_study_shield_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudyMint,
                                contentColor = Color(0xFF092017)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Activate Study Lock ($lockDuration min)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Curated YouTube Study Channels Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Preferred Study Channels",
                        color = StudyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Whitelisted educational channels (Shorts & distractions blocked)",
                        color = StudyTextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showAddChannelDialog = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add preferred channel",
                        tint = StudyMint
                    )
                }
            }
        }

        items(channels, key = { it.id }) { channel ->
            CuratedChannelCard(
                channel = channel,
                onWatchInApp = {
                    val searchSafeUrl = "https://www.youtube.com/results?search_query=" +
                            channel.searchKeyword.replace(" ", "+")
                    inAppViewingUrl = searchSafeUrl
                    inAppViewingTitle = channel.title
                },
                onLaunchExternal = {
                    val searchSafeUrl = "https://www.youtube.com/results?search_query=" +
                            channel.searchKeyword.replace(" ", "+")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchSafeUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open YouTube app", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Distraction Blacklist Manager
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
                        Column {
                            Text(
                                text = "Distraction Blacklist",
                                color = StudyTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Locked during Study Mode to protect focus",
                                color = StudyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Blocked",
                            tint = StudyRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    blockedItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.repository.toggleBlockerItem(item.name) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    color = StudyTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.category,
                                    color = StudyTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = item.isBlocked,
                                onCheckedChange = { viewModel.repository.toggleBlockerItem(item.name) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StudyRed,
                                    checkedTrackColor = StudyRed.copy(alpha = 0.3f),
                                    uncheckedThumbColor = StudyTextMuted,
                                    uncheckedTrackColor = StudyBorder
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Emergency Unlock Dialog
    if (showEmergencyUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyUnlockDialog = false },
            title = {
                Text(
                    text = "Emergency Unlock?",
                    color = StudyRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Disabling Study Shield prematurely will break your focus streak and deduct 50 XP from your scholar rank.\n\nAre you sure you want to exit Study Mode?",
                    color = StudyTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emergencyUnlock()
                        showEmergencyUnlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyRed, contentColor = Color.White)
                ) {
                    Text("Unlock & Deduct 50 XP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyUnlockDialog = false }) {
                    Text("Stay Focused", color = StudyMint)
                }
            },
            containerColor = StudyPanel
        )
    }

    // Add Custom Channel Dialog
    if (showAddChannelDialog) {
        AlertDialog(
            onDismissRequest = { showAddChannelDialog = false },
            title = { Text("Add Preferred Study Channel", color = StudyTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newChannelTitle,
                        onValueChange = { newChannelTitle = it },
                        label = { Text("Channel / Course Name") },
                        placeholder = { Text("e.g. Shomu's Biology") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newChannelEducator,
                        onValueChange = { newChannelEducator = it },
                        label = { Text("Educator / Faculty") },
                        placeholder = { Text("e.g. Suman Bhattacharjee") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newChannelSubject,
                        onValueChange = { newChannelSubject = it },
                        label = { Text("Subject / Topic") },
                        placeholder = { Text("e.g. Genetics & Immunology") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newChannelSearch,
                        onValueChange = { newChannelSearch = it },
                        label = { Text("Search Keyword for Lectures") },
                        placeholder = { Text("e.g. Shomu Biology Immunology") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChannelTitle.isNotBlank()) {
                            viewModel.repository.addCustomChannel(
                                title = newChannelTitle,
                                educator = newChannelEducator.ifEmpty { "Educator" },
                                subject = newChannelSubject.ifEmpty { "Sciences" },
                                searchKeyword = newChannelSearch.ifEmpty { newChannelTitle }
                            )
                            showAddChannelDialog = false
                            newChannelTitle = ""
                            newChannelEducator = ""
                            newChannelSubject = ""
                            newChannelSearch = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyMint, contentColor = Color(0xFF092017))
                ) {
                    Text("Save Channel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannelDialog = false }) {
                    Text("Cancel", color = StudyTextSecondary)
                }
            },
            containerColor = StudyPanel
        )
    }
}

@Composable
fun CuratedChannelCard(
    channel: CuratedStudyChannel,
    onWatchInApp: () -> Unit,
    onLaunchExternal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("channel_card_${channel.id}"),
        colors = CardDefaults.cardColors(containerColor = StudyPanel),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.title,
                        color = StudyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${channel.educator} · ${channel.subject}",
                        color = StudyMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    color = StudySurfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "APPROVED",
                        color = StudyBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = channel.description,
                color = StudyTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Recommended topics pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                channel.recommendedTopics.take(3).forEach { topic ->
                    Surface(
                        color = StudyPanelElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(StudyBorder)
                        )
                    ) {
                        Text(
                            text = topic,
                            color = StudyTextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWatchInApp,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudyMint,
                        contentColor = Color(0xFF092017)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Watch In-App",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Study In-App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onLaunchExternal,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudyTextPrimary),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StudyBorderLight)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Open App",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("YouTube App", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * In-App Educational Web Viewer that blocks Shorts URLs and keeps students inside
 * their lecture without distraction feeds.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EducationalWebStudyViewer(
    url: String,
    channelTitle: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudyBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudyPanel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "STUDY VIEWER (SHORTS BLOCKED)",
                    color = StudyMint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = channelTitle,
                    color = StudyTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            FilledTonalIconButton(
                onClick = onClose,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = StudySurfaceVariant,
                    contentColor = StudyTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Viewer",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Webview with custom client blocking shorts
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val requestUrl = request?.url?.toString() ?: ""
                            // Strictly intercept and block Shorts
                            if (requestUrl.contains("/shorts/", ignoreCase = true)) {
                                Toast.makeText(ctx, "Distraction Shield: Shorts are blocked during Study Mode!", Toast.LENGTH_LONG).show()
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(url)
                }
            }
        )
    }
}
