package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Song
import com.example.ui.theme.BorderDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LeftHandColor
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RightHandColor
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * Defines the physical pixel bounding box of a musical staff system
 * on the scanned sheet music page.
 */
data class SystemBounds(
    val systemIndex: Int,       // 1 to 4
    val startMeasure: Int,      // e.g. 1
    val endMeasure: Int,        // e.g. 3
    val topYNorm: Float,        // normalized 0.0 to 1.0 on bitmap
    val bottomYNorm: Float,     // normalized 0.0 to 1.0 on bitmap
    val leftXNorm: Float,       // normalized 0.0 to 1.0 (after clef/key sig)
    val rightXNorm: Float,      // normalized 0.0 to 1.0 (end barline)
    val startTimestampMs: Long,
    val endTimestampMs: Long
)

/**
 * Interactive, high-resolution Scanned Sheet Music Viewer with a real-time playback
 * tracking cursor that glides through the measures and allows tap-to-seek anywhere.
 */
@Composable
fun ScannedScoreViewer(
    bitmap: Bitmap?,
    song: Song,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isEmbeddedSplitView: Boolean = false
) {
    val context = LocalContext.current
    var scoreBitmap by remember { mutableStateOf<Bitmap?>(bitmap) }
    var tapFeedbackPos by remember { mutableStateOf<Offset?>(null) }

    // Load default score asset if none provided
    LaunchedEffect(bitmap) {
        if (bitmap != null) {
            scoreBitmap = bitmap
        } else if (scoreBitmap == null) {
            try {
                val inputStream = context.assets.open("scores/rain_on_the_window.png")
                scoreBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (_: Exception) {}
        }
    }

    // Clear tap feedback ripple after a brief flash
    LaunchedEffect(tapFeedbackPos) {
        if (tapFeedbackPos != null) {
            kotlinx.coroutines.delay(400)
            tapFeedbackPos = null
        }
    }

    // 4 Musical systems mapped to Page 3 of "Rain on the Window"
    val measureDuration = 2068L // 116 BPM, 4/4
    val systems = remember(song.id, song.durationMs) {
        listOf(
            SystemBounds(
                systemIndex = 1,
                startMeasure = 1,
                endMeasure = 3,
                topYNorm = 0.200f,
                bottomYNorm = 0.345f,
                leftXNorm = 0.230f,
                rightXNorm = 0.925f,
                startTimestampMs = 0L,
                endTimestampMs = measureDuration * 3L
            ),
            SystemBounds(
                systemIndex = 2,
                startMeasure = 4,
                endMeasure = 6,
                topYNorm = 0.360f,
                bottomYNorm = 0.505f,
                leftXNorm = 0.210f,
                rightXNorm = 0.925f,
                startTimestampMs = measureDuration * 3L,
                endTimestampMs = measureDuration * 6L
            ),
            SystemBounds(
                systemIndex = 3,
                startMeasure = 7,
                endMeasure = 8,
                topYNorm = 0.530f,
                bottomYNorm = 0.675f,
                leftXNorm = 0.210f,
                rightXNorm = 0.925f,
                startTimestampMs = measureDuration * 6L,
                endTimestampMs = measureDuration * 8L
            ),
            SystemBounds(
                systemIndex = 4,
                startMeasure = 9,
                endMeasure = 11,
                topYNorm = 0.700f,
                bottomYNorm = 0.850f,
                leftXNorm = 0.230f,
                rightXNorm = 0.925f,
                startTimestampMs = measureDuration * 8L,
                endTimestampMs = song.durationMs.coerceAtLeast(measureDuration * 11L)
            )
        )
    }

    // Determine current system based on playback time
    val currentSystem = systems.find {
        currentPositionMs in it.startTimestampMs..it.endTimestampMs
    } ?: if (currentPositionMs < systems.first().startTimestampMs) systems.first() else systems.last()

    val currentMeasure = 1 + (currentPositionMs / measureDuration).toInt().coerceIn(0, 10)

    val infiniteTransition = rememberInfiniteTransition(label = "cursorGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("scanned_score_viewer")
    ) {
        // --- TOP TOOLBAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldAccent.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Measure $currentMeasure of 11",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "${song.composer} • ${song.keySignature} • 116 BPM • Page 3",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Restart button
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause toggle
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) EmeraldGreen else ElectricCyan)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (!isEmbeddedSplitView) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Tap-to-seek instruction pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0C101A))
                .border(0.5.dp, BorderDark, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap anywhere on score to jump playback to that measure",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = formatTime(currentPositionMs) + " / " + formatTime(song.durationMs),
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- INTERACTIVE SHEET MUSIC CANVAS WITH REAL-TIME TRACKING PROGRESS BAR ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF080B10))
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
        ) {
            val canvasWidth = maxWidth.value
            val canvasHeight = maxHeight.value

            val currentBitmap = scoreBitmap

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(systems, song.durationMs) {
                        detectTapGestures { tapOffset ->
                            tapFeedbackPos = tapOffset
                            if (currentBitmap != null) {
                                val bmpW = currentBitmap.width.toFloat()
                                val bmpH = currentBitmap.height.toFloat()

                                // Compute aspect-fit destination rect
                                val scale = minOf(size.width / bmpW, size.height / bmpH)
                                val drawW = bmpW * scale
                                val drawH = bmpH * scale
                                val offsetX = (size.width - drawW) / 2f
                                val offsetY = (size.height - drawH) / 2f

                                // Check if click is inside image
                                if (tapOffset.x in offsetX..(offsetX + drawW) &&
                                    tapOffset.y in offsetY..(offsetY + drawH)
                                ) {
                                    val normX = (tapOffset.x - offsetX) / drawW
                                    val normY = (tapOffset.y - offsetY) / drawH

                                    // Find closest system to tap Y
                                    val clickedSys = systems.minByOrNull { sys ->
                                        val sysMidY = (sys.topYNorm + sys.bottomYNorm) / 2f
                                        abs(normY - sysMidY)
                                    }

                                    if (clickedSys != null) {
                                        val progress = ((normX - clickedSys.leftXNorm) / (clickedSys.rightXNorm - clickedSys.leftXNorm))
                                            .coerceIn(0f, 1f)
                                        val targetMs = clickedSys.startTimestampMs +
                                                (progress * (clickedSys.endTimestampMs - clickedSys.startTimestampMs)).toLong()
                                        onSeek(targetMs.coerceIn(0L, song.durationMs))
                                    }
                                }
                            }
                        }
                    }
                    .testTag("interactive_sheet_music_canvas")
            ) {
                if (currentBitmap != null) {
                    val bmpW = currentBitmap.width.toFloat()
                    val bmpH = currentBitmap.height.toFloat()

                    // Aspect-fit image calculation
                    val scale = minOf(size.width / bmpW, size.height / bmpH)
                    val drawW = bmpW * scale
                    val drawH = bmpH * scale
                    val imgLeft = (size.width - drawW) / 2f
                    val imgTop = (size.height - drawH) / 2f

                    // 1. Draw the scanned score image
                    drawImage(
                        image = currentBitmap.asImageBitmap(),
                        dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )

                    // 2. Draw system guide outlines for clarity
                    for (sys in systems) {
                        val isCurrent = sys.systemIndex == currentSystem.systemIndex
                        val sysX = imgLeft + (sys.leftXNorm * drawW)
                        val sysY = imgTop + (sys.topYNorm * drawH)
                        val sysW = (sys.rightXNorm - sys.leftXNorm) * drawW
                        val sysH = (sys.bottomYNorm - sys.topYNorm) * drawH

                        if (isCurrent) {
                            // Active system aura
                            drawRoundRect(
                                color = ElectricCyan.copy(alpha = 0.08f),
                                topLeft = Offset(sysX, sysY),
                                size = Size(sysW, sysH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }

                    // 3. Draw the live real-time progress bar through the score!
                    val sysProgress = ((currentPositionMs - currentSystem.startTimestampMs).toFloat() /
                            (currentSystem.endTimestampMs - currentSystem.startTimestampMs).toFloat())
                        .coerceIn(0f, 1f)

                    val barX = imgLeft + ((currentSystem.leftXNorm + (sysProgress * (currentSystem.rightXNorm - currentSystem.leftXNorm))) * drawW)
                    val barTopY = imgTop + (currentSystem.topYNorm * drawH)
                    val barBottomY = imgTop + (currentSystem.bottomYNorm * drawH)
                    val barH = barBottomY - barTopY

                    // Soft luminous glow halo behind the tracking bar
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ElectricCyan.copy(alpha = 0.0f),
                                ElectricCyan.copy(alpha = 0.35f * glowAlpha),
                                ElectricCyan.copy(alpha = 0.0f)
                            ),
                            startX = barX - 18f,
                            endX = barX + 18f
                        ),
                        topLeft = Offset(barX - 18f, barTopY - 6f),
                        size = Size(36f, barH + 12f)
                    )

                    // Sharp primary laser tracking line
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                ElectricCyan,
                                RightHandColor
                            ),
                            startY = barTopY,
                            endY = barBottomY
                        ),
                        start = Offset(barX, barTopY - 4f),
                        end = Offset(barX, barBottomY + 4f),
                        strokeWidth = 3.5f
                    )

                    // Top indicator head badge (glowing diamond / bead)
                    drawCircle(
                        color = Color.White,
                        radius = 5.5f,
                        center = Offset(barX, barTopY - 4f)
                    )
                    drawCircle(
                        color = ElectricCyan,
                        radius = 8.5f,
                        center = Offset(barX, barTopY - 4f),
                        style = Stroke(width = 2f)
                    )

                    // Bottom anchor bead
                    drawCircle(
                        color = ElectricCyan,
                        radius = 4.5f,
                        center = Offset(barX, barBottomY + 4f)
                    )

                    // 4. Tap ripple animation feedback
                    tapFeedbackPos?.let { tap ->
                        drawCircle(
                            color = GoldAccent.copy(alpha = 0.7f),
                            radius = 24f,
                            center = tap,
                            style = Stroke(width = 3f)
                        )
                    }
                } else {
                    // Fallback loading indicator
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        size = size
                    )
                }
            }
        }
    }
}

/**
 * Modal dialog wrapper for displaying the interactive sheet music viewer.
 */
@Composable
fun ScannedScoreViewerDialog(
    bitmap: Bitmap?,
    song: Song,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, BorderDark, RoundedCornerShape(16.dp)),
            color = ObsidianBlack
        ) {
            ScannedScoreViewer(
                bitmap = bitmap,
                song = song,
                currentPositionMs = currentPositionMs,
                isPlaying = isPlaying,
                onPlayPauseToggle = onPlayPauseToggle,
                onRestart = onRestart,
                onSeek = onSeek,
                onClose = onDismiss,
                isEmbeddedSplitView = false
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
