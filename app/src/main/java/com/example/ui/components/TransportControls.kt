package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TransportControls(
    isPlaying: Boolean,
    isPracticeMode: Boolean,
    isMetronomeActive: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    playbackSpeed: Float,
    bpm: Int,
    onPlayPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onPracticeModeToggle: (Boolean) -> Unit,
    onMetronomeToggle: () -> Unit,
    onSpeedChange: (speed: Float) -> Unit,
    onOpenSongList: () -> Unit,
    onOpenScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Timeline Scrubber Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPositionMs),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(42.dp)
            )

            Slider(
                value = currentPositionMs.toFloat().coerceIn(0f, maxOf(1f, totalDurationMs.toFloat())),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxOf(1f, totalDurationMs.toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = if (isPracticeMode) ElectricCyan else EmeraldGreen,
                    activeTrackColor = if (isPracticeMode) ElectricCyan else EmeraldGreen,
                    inactiveTrackColor = SurfaceElevated
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("timeline_scrubber")
            )

            Text(
                text = formatTime(totalDurationMs),
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(42.dp)
            )
        }

        // Main Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Song Picker & Sheet Music Scan
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenSongList,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("song_list_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Song Library",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onOpenScanner,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("scan_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan Sheet Music",
                        tint = ElectricCyan
                    )
                }
            }

            // Center Group: Rewind, Primary Play/Pause, Speed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart Song",
                        tint = TextSecondary
                    )
                }

                // Primary Play/Pause Floating Action Button
                FilledIconButton(
                    onClick = onPlayPauseToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPracticeMode) ElectricCyan else EmeraldGreen,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Speed Cycle Button (0.5x -> 0.75x -> 1.0x -> 1.25x -> 1.5x)
                ElevatedButton(
                    onClick = {
                        val nextSpeed = when (playbackSpeed) {
                            0.5f -> 0.75f
                            0.75f -> 1.0f
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            else -> 0.5f
                        }
                        onSpeedChange(nextSpeed)
                    },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = SurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("speed_cycle_button")
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Group: Practice Mode Switch & Metronome
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metronome Button
                IconButton(
                    onClick = onMetronomeToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isMetronomeActive) GoldAccent.copy(alpha = 0.2f) else Color.Transparent)
                        .testTag("metronome_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Metronome",
                        tint = if (isMetronomeActive) GoldAccent else TextMuted
                    )
                }

                // Practice Mode Toggle Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPracticeMode) ElectricCyan.copy(alpha = 0.2f) else SurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (isPracticeMode) ElectricCyan else BorderDark,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onPracticeModeToggle(!isPracticeMode) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("practice_mode_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Practice Mode",
                            tint = if (isPracticeMode) ElectricCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Practice",
                            color = if (isPracticeMode) ElectricCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isPracticeMode) ElectricCyan else Color(0xFF283142))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isPracticeMode) "ON" else "OFF",
                                color = if (isPracticeMode) Color.Black else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
