package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyVisualState
import com.example.model.NoteUtils
import com.example.ui.theme.BorderDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated

/**
 * Miniature 88-key panoramic navigation strip showing the entire piano range (A0 - C8)
 * with interactive camera viewport framing and quick octave jump presets.
 */
@Composable
fun OctaveNavigator(
    currentViewportStart: Int, // 0 to 51
    visibleWhiteCount: Int,     // e.g. 18 to 32
    keyStates: List<KeyVisualState>,
    onViewportChange: (newStart: Int) -> Unit,
    pieceLowestMidi: Int? = null,
    pieceHighestMidi: Int? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Octave Jump Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val octaves = listOf(
                "C1" to 2,   // A0-B0 is 0-1, C1 starts around 2
                "C2" to 9,
                "C3" to 16,
                "C4 (Mid)" to 23,
                "C5" to 30,
                "C6" to 37,
                "C7" to 44
            )

            octaves.forEach { (label, startKey) ->
                val isActive = currentViewportStart in (startKey - 4)..(startKey + 4)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isActive) ElectricCyan.copy(alpha = 0.2f) else SurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (isActive) ElectricCyan else BorderDark,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onViewportChange(startKey.coerceIn(0, 52 - visibleWhiteCount)) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("octave_pill_$label")
                ) {
                    Text(
                        text = label,
                        color = if (isActive) ElectricCyan else Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Full 88-Key Mini Panoramic Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0D14))
                .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                .pointerInput(currentViewportStart, visibleWhiteCount) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetStart = (ratio * 52f - (visibleWhiteCount / 2f)).toInt()
                        onViewportChange(targetStart.coerceIn(0, 52 - visibleWhiteCount))
                    }
                }
                .pointerInput(currentViewportStart, visibleWhiteCount) {
                    detectDragGestures { change, _ ->
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        val targetStart = (ratio * 52f - (visibleWhiteCount / 2f)).toInt()
                        onViewportChange(targetStart.coerceIn(0, 52 - visibleWhiteCount))
                    }
                }
                .testTag("panoramic_keyboard_strip")
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val totalWhite = 52
                val keyW = size.width / totalWhite.toFloat()

                // 1. Draw mini white keys
                for (w in 0 until totalWhite) {
                    val x = w * keyW
                    drawRect(
                        color = Color(0xFFD1D5DB),
                        topLeft = Offset(x + 0.5f, 0f),
                        size = Size(keyW - 1f, size.height)
                    )
                }

                // 2. Draw mini black keys
                for (midi in 21..108) {
                    if (!NoteUtils.isWhiteKey(midi)) {
                        val whiteIdx = getWhiteKeyIndexForMidi(midi)
                        val x = (whiteIdx * keyW) - (keyW * 0.3f)
                        drawRect(
                            color = Color(0xFF1E222B),
                            topLeft = Offset(x, 0f),
                            size = Size(keyW * 0.6f, size.height * 0.58f)
                        )
                    }
                }

                // 3. Highlight keys with active notes or target cues
                for (state in keyStates) {
                    if (state.isTargetCue || state.isPlaying || state.isHitSuccess) {
                        val whiteIdx = getWhiteKeyIndexForMidi(state.midiNumber)
                        val x = whiteIdx * keyW
                        val cueColor = when {
                            state.isHitSuccess -> GoldAccent
                            state.isTargetCue -> ElectricCyan
                            else -> EmeraldGreen
                        }
                        drawCircle(
                            color = cueColor,
                            radius = 3.5f,
                            center = Offset(x + keyW / 2f, size.height * 0.75f)
                        )
                    }
                }

                // 4. Draw piece extents range indicators (lowest note to highest note)
                if (pieceLowestMidi != null && pieceHighestMidi != null) {
                    val minW = getWhiteKeyIndexForMidi(pieceLowestMidi)
                    val maxW = getWhiteKeyIndexForMidi(pieceHighestMidi)
                    val pieceLeft = (minW / 52f) * size.width
                    val pieceRight = ((maxW + 1f) / 52f) * size.width
                    // Range underline
                    drawLine(
                        color = GoldAccent.copy(alpha = 0.85f),
                        start = Offset(pieceLeft, size.height - 2f),
                        end = Offset(pieceRight, size.height - 2f),
                        strokeWidth = 2.5f
                    )
                    // Lowest note marker
                    drawCircle(
                        color = GoldAccent,
                        radius = 2.5f,
                        center = Offset(pieceLeft, size.height - 2f)
                    )
                    // Highest note marker
                    drawCircle(
                        color = GoldAccent,
                        radius = 2.5f,
                        center = Offset(pieceRight, size.height - 2f)
                    )
                }

                // 5. Draw active camera viewport frame
                val frameLeft = (currentViewportStart / 52f) * size.width
                val frameWidth = (visibleWhiteCount / 52f) * size.width

                drawRoundRect(
                    color = ElectricCyan.copy(alpha = 0.25f),
                    topLeft = Offset(frameLeft, 0f),
                    size = Size(frameWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                drawRoundRect(
                    color = ElectricCyan,
                    topLeft = Offset(frameLeft, 0f),
                    size = Size(frameWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
