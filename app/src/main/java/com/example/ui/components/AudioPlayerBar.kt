package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SoundType
import com.example.ui.theme.*
import com.example.viewmodel.StudyViewModel

@Composable
fun AudioPlayerBar(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.audioSynthesizer.isPlaying.collectAsState()
    val currentSound by viewModel.audioSynthesizer.currentSound.collectAsState()
    val volume by viewModel.audioSynthesizer.volume.collectAsState()
    var showExpandedControls by remember { mutableStateOf(false) }

    val track = viewModel.ambientTracks.find { it.type == currentSound }
        ?: viewModel.ambientTracks.first()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showExpandedControls = !showExpandedControls }
            .testTag("audio_player_bar"),
        colors = CardDefaults.cardColors(
            containerColor = StudyPanelElevated
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isPlaying) StudyMint.copy(alpha = 0.5f) else StudyBorder
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(if (isPlaying) pulseScale else 1f)
                            .background(
                                if (isPlaying) StudyMint.copy(alpha = 0.2f) else StudySurfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                            contentDescription = "Ambient Audio",
                            tint = if (isPlaying) StudyMint else StudyTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.title,
                                color = StudyTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StudySurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = track.tag,
                                    color = StudyBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isPlaying) "Playing · Copyright-Free Ambient" else "Tap to choose study soundscape",
                            color = StudyTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        viewModel.audioSynthesizer.toggle(currentSound)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("audio_play_toggle_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) StudyMint else StudySurfaceVariant,
                        contentColor = if (isPlaying) Color(0xFF092017) else StudyTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Audio" else "Play Audio"
                    )
                }
            }

            AnimatedVisibility(visible = showExpandedControls) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "SELECT SOOTHING SOUNDSCAPE (NO COPYRIGHT)",
                        color = StudyTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid of ambient sound choices
                    viewModel.ambientTracks.chunked(2).forEach { rowTracks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTracks.forEach { item ->
                                val isSelected = currentSound == item.type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.audioSynthesizer.play(item.type)
                                    },
                                    label = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = item.title,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = item.frequencyLabel,
                                                fontSize = 10.sp,
                                                color = if (isSelected) StudyMint else StudyTextMuted
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
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
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = StudyTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = { viewModel.audioSynthesizer.setVolume(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("audio_volume_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = StudyMint,
                                activeTrackColor = StudyMint,
                                inactiveTrackColor = StudyBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            color = StudyTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
