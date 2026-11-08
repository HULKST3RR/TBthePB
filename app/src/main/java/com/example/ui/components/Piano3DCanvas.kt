package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.Hand
import com.example.model.KeyVisualState
import com.example.model.NoteEvent
import com.example.model.NoteUtils
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.KeyShadow
import com.example.ui.theme.LeftHandColor
import com.example.ui.theme.RightHandColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Perspective-rendered interactive acoustic piano keyboard
 * with falling Synthesia-style note trails, key pivot physics,
 * and dynamic state illumination.
 */
@Composable
fun Piano3DCanvas(
    keyStates: List<KeyVisualState>,
    activeNotes: List<NoteEvent>,
    currentPositionMs: Long,
    viewportStartKey: Int, // First visible white key index (0 to 51)
    visibleWhiteKeysCount: Int = 18, // Visible span of white keys
    onKeyPress: (midi: Int) -> Unit,
    onKeyRelease: (midi: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var touchedMidi by remember { mutableStateOf<Int?>(null) }

    // Smooth camera viewport panning
    val animatedViewportStart by animateFloatAsState(
        targetValue = viewportStartKey.toFloat(),
        animationSpec = tween(durationMillis = 350),
        label = "viewportPan"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(keyStates, animatedViewportStart) {
                detectTapGestures(
                    onPress = { offset ->
                        val midi = hitTestKey(offset, size.width.toFloat(), size.height.toFloat(), animatedViewportStart, visibleWhiteKeysCount)
                        if (midi != null) {
                            touchedMidi = midi
                            onKeyPress(midi)
                            tryAwaitRelease()
                            onKeyRelease(midi)
                            touchedMidi = null
                        }
                    }
                )
            }
            .pointerInput(keyStates, animatedViewportStart) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val midi = hitTestKey(offset, size.width.toFloat(), size.height.toFloat(), animatedViewportStart, visibleWhiteKeysCount)
                        if (midi != null) {
                            touchedMidi = midi
                            onKeyPress(midi)
                        }
                    },
                    onDrag = { change, _ ->
                        val newMidi = hitTestKey(change.position, size.width.toFloat(), size.height.toFloat(), animatedViewportStart, visibleWhiteKeysCount)
                        if (newMidi != null && newMidi != touchedMidi) {
                            touchedMidi?.let { onKeyRelease(it) }
                            touchedMidi = newMidi
                            onKeyPress(newMidi)
                        }
                    },
                    onDragEnd = {
                        touchedMidi?.let { onKeyRelease(it) }
                        touchedMidi = null
                    },
                    onDragCancel = {
                        touchedMidi?.let { onKeyRelease(it) }
                        touchedMidi = null
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        // 3D Perspective camera parameters
        val horizonY = height * 0.12f
        val pianoTopY = height * 0.58f
        val pianoBottomY = height * 0.94f
        val keybedFrontZ = 1.0f
        val keybedBackZ = 2.4f
        val trailHorizonZ = 6.0f

        // Draw concert hall background ambient depth
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF06080E),
                    Color(0xFF0F1420),
                    Color(0xFF141A29)
                )
            ),
            size = size
        )

        // Draw Synthesia falling note ribbons in 3D perspective
        drawFallingNoteTrails(
            activeNotes = activeNotes,
            currentPositionMs = currentPositionMs,
            width = width,
            height = height,
            horizonY = horizonY,
            pianoTopY = pianoTopY,
            viewportStart = animatedViewportStart,
            visibleWhiteCount = visibleWhiteKeysCount,
            keyStates = keyStates
        )

        // Draw 3D piano body chassis & keybed shadow
        drawPianoChassis(
            width = width,
            height = height,
            pianoTopY = pianoTopY,
            pianoBottomY = pianoBottomY
        )

        // Draw White Keys (Layer 1)
        drawWhiteKeys(
            keyStates = keyStates,
            width = width,
            height = height,
            pianoTopY = pianoTopY,
            pianoBottomY = pianoBottomY,
            viewportStart = animatedViewportStart,
            visibleWhiteCount = visibleWhiteKeysCount,
            touchedMidi = touchedMidi
        )

        // Draw Black Keys (Layer 2 - elevated in 3D)
        drawBlackKeys(
            keyStates = keyStates,
            width = width,
            height = height,
            pianoTopY = pianoTopY,
            pianoBottomY = pianoBottomY,
            viewportStart = animatedViewportStart,
            visibleWhiteCount = visibleWhiteKeysCount,
            touchedMidi = touchedMidi
        )
    }
}

/**
 * Draws falling note visualizer trails that descend from the horizon into the keyboard.
 */
private fun DrawScope.drawFallingNoteTrails(
    activeNotes: List<NoteEvent>,
    currentPositionMs: Long,
    width: Float,
    height: Float,
    horizonY: Float,
    pianoTopY: Float,
    viewportStart: Float,
    visibleWhiteCount: Int,
    keyStates: List<KeyVisualState>
) {
    val lookaheadMs = 2800L
    val upcomingNotes = activeNotes.filter {
        it.timestampMs <= currentPositionMs + lookaheadMs && it.endTimeMs >= currentPositionMs - 300L
    }

    for (note in upcomingNotes) {
        val keyState = keyStates.find { it.midiNumber == note.midiNumber } ?: continue
        val whiteKeyIndex = getWhiteKeyIndexForMidi(note.midiNumber)

        // Map x position relative to viewport
        val relPos = (whiteKeyIndex - viewportStart) / visibleWhiteCount.toFloat()
        if (relPos < -0.15f || relPos > 1.15f) continue

        val xCenter = width * (0.05f + relPos * 0.90f)

        // Time to 3D Z distance calculation
        val timeUntilHit = note.timestampMs - currentPositionMs
        val timeSinceEnd = currentPositionMs - note.endTimeMs

        val tNormStart = (timeUntilHit.toFloat() / lookaheadMs).coerceIn(0f, 1f)
        val tNormEnd = ((timeUntilHit + note.durationMs).toFloat() / lookaheadMs).coerceIn(0f, 1f)

        // Non-linear perspective scaling for falling speed realism
        val yBottom = pianoTopY - ((1f - tNormStart) * (pianoTopY - horizonY))
        val yTop = pianoTopY - ((1f - tNormEnd) * (pianoTopY - horizonY))

        if (yTop >= yBottom) continue

        val trailWidth = (width / visibleWhiteCount) * (0.35f + (yBottom / pianoTopY) * 0.45f)

        // Left Hand vs Right Hand distinct color coding
        val handColor = if (note.hand == Hand.LEFT) LeftHandColor else RightHandColor
        val baseColor = when {
            keyState.isHitSuccess -> GoldAccent
            keyState.isTargetCue -> handColor
            else -> handColor
        }

        val trailBrush = Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.15f),
                baseColor.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.95f)
            ),
            startY = yTop,
            endY = yBottom
        )

        drawRoundRect(
            brush = trailBrush,
            topLeft = Offset(xCenter - trailWidth / 2f, yTop),
            size = Size(trailWidth, (yBottom - yTop).coerceAtLeast(8f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
    }
}

/**
 * Draws the concert grand piano wooden rail and felt backing.
 */
private fun DrawScope.drawPianoChassis(
    width: Float,
    height: Float,
    pianoTopY: Float,
    pianoBottomY: Float
) {
    // Red felt damper strip above keys
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF5E0D0D),
                Color(0xFF8B1212),
                Color(0xFF380707)
            )
        ),
        topLeft = Offset(0f, pianoTopY - 14f),
        size = Size(width, 16f)
    )

    // Brushed brass nameplate bar
    drawLine(
        color = Color(0xFFD4AF37),
        start = Offset(0f, pianoTopY - 14f),
        end = Offset(width, pianoTopY - 14f),
        strokeWidth = 2.5f
    )

    // Keybed lower chassis shadow
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F1219),
                Color(0xFF05070A)
            )
        ),
        topLeft = Offset(0f, pianoBottomY),
        size = Size(width, height - pianoBottomY)
    )
}

/**
 * Draws the 52 white keys with perspective bevels and dynamic lighting.
 */
private fun DrawScope.drawWhiteKeys(
    keyStates: List<KeyVisualState>,
    width: Float,
    height: Float,
    pianoTopY: Float,
    pianoBottomY: Float,
    viewportStart: Float,
    visibleWhiteCount: Int,
    touchedMidi: Int?
) {
    val keyWidth = (width * 0.92f) / visibleWhiteCount
    val keyHeight = pianoBottomY - pianoTopY

    for (w in 0 until 52) {
        val relIndex = w - viewportStart
        if (relIndex < -1 || relIndex > visibleWhiteCount + 1) continue

        val xLeft = width * 0.04f + (relIndex * keyWidth)
        val xRight = xLeft + keyWidth

        val midi = getMidiForWhiteKeyIndex(w)
        val state = keyStates.find { it.midiNumber == midi }
        val isDepressed = state?.isPlaying == true || state?.isHitSuccess == true || state?.isTargetCue == true || touchedMidi == midi

        // Physical pivot displacement (front drops by 10-14px when pressed)
        val pivotDrop = if (isDepressed) 10f else 0f
        val frontY = pianoBottomY + pivotDrop
        val backY = pianoTopY

        // Key face lighting color with Left Hand (Magenta) vs Right Hand (Cyan) differentiation
        val keyFillBrush = when {
            state?.isHitSuccess == true -> Brush.verticalGradient(
                colors = listOf(Color(0xFFDCFCE7), Color(0xFF86EFAC), EmeraldGreen)
            )
            state?.isTargetCue == true -> {
                val cueColor = if (state.activeHand == Hand.LEFT) LeftHandColor else RightHandColor
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), cueColor.copy(alpha = 0.5f), cueColor)
                )
            }
            state?.isPlaying == true -> {
                if (state.activeHand == Hand.LEFT) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFDF4FF), Color(0xFFF0ABFC), LeftHandColor)
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF0FDF4), Color(0xFF7DD3FC), RightHandColor)
                    )
                }
            }
            touchedMidi == midi -> Brush.verticalGradient(
                colors = listOf(Color(0xFFFEF3C7), Color(0xFFFCD34D), GoldAccent)
            )
            state?.isMicDetected == true -> Brush.verticalGradient(
                colors = listOf(Color(0xFFFFEDD5), Color(0xFFFB923C))
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF3F4F6),
                    Color(0xFFE5E7EB)
                )
            )
        }

        // Draw 3D top surface of white key
        val keyPath = Path().apply {
            moveTo(xLeft + 1f, backY)
            lineTo(xRight - 1f, backY)
            lineTo(xRight - 1f, frontY - 12f)
            lineTo(xLeft + 1f, frontY - 12f)
            close()
        }
        drawPath(keyPath, keyFillBrush)

        // Draw 3D front lip of white key (the vertical front edge facing the pianist)
        val lipPath = Path().apply {
            moveTo(xLeft + 1f, frontY - 12f)
            lineTo(xRight - 1f, frontY - 12f)
            lineTo(xRight - 1f, frontY)
            lineTo(xLeft + 1f, frontY)
            close()
        }
        val lipColor = if (isDepressed) Color(0xFFB5BAC3) else Color(0xFFCBD5E1)
        drawPath(lipPath, lipColor)

        // Bevel divider line between white keys
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(xRight, backY),
            end = Offset(xRight, frontY),
            strokeWidth = 1.5f
        )

        // Subtle ambient drop shadow on depressed key
        if (isDepressed) {
            drawRect(
                color = KeyShadow,
                topLeft = Offset(xLeft, backY),
                size = Size(xRight - xLeft, 14f)
            )
        }
    }
}

/**
 * Draws the 36 black keys elevated in 3D above the white keys.
 */
private fun DrawScope.drawBlackKeys(
    keyStates: List<KeyVisualState>,
    width: Float,
    height: Float,
    pianoTopY: Float,
    pianoBottomY: Float,
    viewportStart: Float,
    visibleWhiteCount: Int,
    touchedMidi: Int?
) {
    val whiteKeyWidth = (width * 0.92f) / visibleWhiteCount
    val blackKeyWidth = whiteKeyWidth * 0.58f
    val blackKeyHeight = (pianoBottomY - pianoTopY) * 0.62f

    val blackKeyOffsets = listOf(0, 1, 3, 4, 5) // Within octave positions

    for (midi in 21..108) {
        if (NoteUtils.isWhiteKey(midi)) continue

        val state = keyStates.find { it.midiNumber == midi }
        val whiteKeyIndex = getWhiteKeyIndexForMidi(midi)

        val relIndex = whiteKeyIndex - viewportStart
        if (relIndex < -1 || relIndex > visibleWhiteCount + 1) continue

        val xCenter = width * 0.04f + (relIndex * whiteKeyWidth)
        val xLeft = xCenter - (blackKeyWidth / 2f)
        val xRight = xLeft + blackKeyWidth

        val isDepressed = state?.isPlaying == true || state?.isHitSuccess == true || state?.isTargetCue == true || touchedMidi == midi
        val pivotDrop = if (isDepressed) 8f else 0f

        val topY = pianoTopY
        val frontY = pianoTopY + blackKeyHeight + pivotDrop

        // Black key body colors with Left Hand (Magenta) vs Right Hand (Cyan) differentiation
        val blackBrush = when {
            state?.isHitSuccess == true -> Brush.verticalGradient(
                colors = listOf(Color(0xFF047857), EmeraldGreen)
            )
            state?.isTargetCue == true -> {
                val cueColor = if (state.activeHand == Hand.LEFT) LeftHandColor else RightHandColor
                Brush.verticalGradient(
                    colors = listOf(cueColor.copy(alpha = 0.6f), cueColor)
                )
            }
            state?.isPlaying == true -> {
                if (state.activeHand == Hand.LEFT) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF86198F), LeftHandColor)
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0369A1), RightHandColor)
                    )
                }
            }
            touchedMidi == midi -> Brush.verticalGradient(
                colors = listOf(Color(0xFFB45309), GoldAccent)
            )
            state?.isMicDetected == true -> Brush.verticalGradient(
                colors = listOf(Color(0xFFC2410C), Color(0xFFF97316))
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2B303C),
                    Color(0xFF151820),
                    Color(0xFF0B0D12)
                )
            )
        }

        // Drop shadow cast by elevated black key onto neighboring white keys
        drawRect(
            color = KeyShadow,
            topLeft = Offset(xLeft - 3f, topY + 4f),
            size = Size(blackKeyWidth + 6f, blackKeyHeight + 8f)
        )

        // 3D Top surface of black key
        drawRoundRect(
            brush = blackBrush,
            topLeft = Offset(xLeft, topY),
            size = Size(blackKeyWidth, frontY - topY),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // Top specular highlight line
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(xLeft + 2f, topY + 2f),
            end = Offset(xRight - 2f, topY + 2f),
            strokeWidth = 1.2f
        )
    }
}

/**
 * Calculates hit test from screen touch coordinates to MIDI key.
 */
private fun hitTestKey(
    offset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    viewportStart: Float,
    visibleWhiteCount: Int
): Int? {
    val pianoTopY = canvasHeight * 0.58f
    val pianoBottomY = canvasHeight * 0.94f
    if (offset.y < pianoTopY || offset.y > pianoBottomY + 20f) return null

    val whiteKeyWidth = (canvasWidth * 0.92f) / visibleWhiteCount
    val blackKeyHeight = (pianoBottomY - pianoTopY) * 0.62f

    // First check elevated Black Keys (if touch is in the upper part of keybed)
    if (offset.y < pianoTopY + blackKeyHeight) {
        val blackKeyWidth = whiteKeyWidth * 0.58f
        for (midi in 21..108) {
            if (NoteUtils.isWhiteKey(midi)) continue
            val whiteIndex = getWhiteKeyIndexForMidi(midi)
            val relIndex = whiteIndex - viewportStart
            val xCenter = canvasWidth * 0.04f + (relIndex * whiteKeyWidth)
            if (offset.x in (xCenter - blackKeyWidth / 2f)..(xCenter + blackKeyWidth / 2f)) {
                return midi
            }
        }
    }

    // Then check White Keys
    val relativeX = offset.x - (canvasWidth * 0.04f)
    val whiteIndex = (viewportStart + (relativeX / whiteKeyWidth)).toInt().coerceIn(0, 51)
    return getMidiForWhiteKeyIndex(whiteIndex)
}

/**
 * Maps 0..51 white key index to MIDI note.
 */
fun getMidiForWhiteKeyIndex(w: Int): Int {
    if (w == 0) return 21 // A0
    if (w == 1) return 23 // B0
    var whiteCount = 2
    for (midi in 24..108) {
        if (NoteUtils.isWhiteKey(midi)) {
            if (whiteCount == w) return midi
            whiteCount++
        }
    }
    return 108 // C8
}

/**
 * Maps any MIDI note (21..108) to its white key reference index (including fractional for black keys).
 */
fun getWhiteKeyIndexForMidi(midi: Int): Float {
    if (midi <= 21) return 0f
    if (midi == 22) return 0.5f // A#0
    if (midi == 23) return 1f   // B0

    var whiteIndex = 2
    for (m in 24..108) {
        if (m == midi) return whiteIndex.toFloat()
        if (!NoteUtils.isWhiteKey(m)) {
            // Black key is between previous white and next white
            if (m == midi) return whiteIndex - 0.5f
        } else {
            whiteIndex++
        }
    }

    // Specific black key fractional placement
    val noteInOctave = midi % 12
    val baseMidi = midi - 1
    val baseWhite = getWhiteKeyIndexForMidi(baseMidi)
    return baseWhite + 0.5f
}
