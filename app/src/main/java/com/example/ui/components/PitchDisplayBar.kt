package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteEvent
import com.example.model.PitchResult
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun PitchDisplayBar(
    pitchResult: PitchResult,
    isPracticeMode: Boolean,
    currentTargetNote: NoteEvent?,
    isCorrectPitchMatched: Boolean,
    isMicPermissionGranted: Boolean,
    toleranceCents: Float,
    onAdjustTolerance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark.copy(alpha = 0.95f))
            .border(1.dp, BorderDark)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Practice Mode Prominent Cue Banner
        if (isPracticeMode && currentTargetNote != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCorrectPitchMatched) EmeraldGreen.copy(alpha = 0.25f)
                        else ElectricCyan.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isCorrectPitchMatched) EmeraldGreen else ElectricCyan.copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isCorrectPitchMatched) EmeraldGreen else ElectricCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCorrectPitchMatched) "HIT! PERFECT NOTE" else "PLAY NOTE (or tap key):",
                        color = if (isCorrectPitchMatched) EmeraldGreen else ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Target Note Badge (e.g. C4, F#5)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentTargetNote.noteName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(MIDI ${currentTargetNote.midiNumber})",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Live Microphone Pitch & Tuner Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Mic Status & Note Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isMicPermissionGranted) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Microphone",
                    tint = if (pitchResult.isDetected) GoldAccent else TextMuted,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (pitchResult.isDetected) pitchResult.noteName else "--",
                    color = if (pitchResult.isDetected) GoldAccent else TextMuted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (pitchResult.isDetected) {
                        String.format("%.1f Hz", pitchResult.frequencyHz)
                    } else {
                        "Listening..."
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Center: Tuning Cents Deviation Indicator
            if (pitchResult.isDetected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val cents = pitchResult.centsOffset
                    val isTuned = abs(cents) <= toleranceCents
                    val tuningColor = if (isTuned) EmeraldGreen else CoralRed

                    Text(
                        text = String.format("%+d cents", cents.toInt()),
                        color = tuningColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Right: Tolerance Config & RMS Energy
            Row(verticalAlignment = Alignment.CenterVertically) {
                // RMS Volume meter bar
                val rmsNormalized = ((pitchResult.rmsDb + 60f) / 60f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { if (pitchResult.isDetected) rmsNormalized else 0.05f },
                    modifier = Modifier
                        .width(44.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (pitchResult.isDetected) GoldAccent else TextMuted,
                    trackColor = SurfaceElevated
                )

                IconButton(
                    onClick = onAdjustTolerance,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("pitch_tolerance_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tolerance Setting",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
