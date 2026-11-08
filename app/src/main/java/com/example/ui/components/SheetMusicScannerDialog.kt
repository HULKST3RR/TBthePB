package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.model.Song
import com.example.omr.OmrService
import com.example.ui.theme.BorderDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SheetMusicScannerDialog(
    omrService: OmrService,
    onSongScanned: (Song) -> Unit,
    onBitmapScanned: ((Bitmap) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Position sheet music inside document guide") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { capturedBitmap ->
        if (capturedBitmap != null) {
            onBitmapScanned?.invoke(capturedBitmap)
            isProcessing = true
            statusText = "Analyzing staves & extracting notes with Gemini OMR..."
            scope.launch {
                val result = omrService.transcribeSheetMusic(capturedBitmap, "")
                isProcessing = false
                result.onSuccess { song ->
                    onSongScanned(song)
                    onDismiss()
                }.onFailure {
                    statusText = "Error scanning. Try another angle or lighting."
                }
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                statusText = "Unable to start camera. Please use Gallery."
            }
        } else {
            statusText = "Camera permission needed to take a photo. Use Gallery or grant permission."
        }
    }

    val launchCameraSafely = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                statusText = "Unable to start camera. Please use Gallery."
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo Picker Launcher (Google Play zero-permission compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            statusText = "Reading score & auto-detecting title..."
            scope.launch {
                val bitmap = try {
                    val stream = context.contentResolver.openInputStream(uri)
                    android.graphics.BitmapFactory.decodeStream(stream)
                } catch (_: Exception) { null }

                if (bitmap != null) {
                    onBitmapScanned?.invoke(bitmap)
                    val result = omrService.transcribeSheetMusic(bitmap, "")
                    isProcessing = false
                    result.onSuccess { song ->
                        onSongScanned(song)
                        onDismiss()
                    }
                } else {
                    isProcessing = false
                    statusText = "Failed to load image"
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scannerLaser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserLine"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sheet Music Scanner",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_scanner_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Document Framing Viewport with Laser Scanning Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090C12))
                        .border(1.5.dp, if (isProcessing) ElectricCyan else BorderDark, RoundedCornerShape(12.dp))
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw musical staff line guides
                        val staffSpacing = 16f
                        val centerY = h / 2f
                        for (i in -2..2) {
                            val y = centerY + (i * staffSpacing)
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = androidx.compose.ui.geometry.Offset(24f, y),
                                end = androidx.compose.ui.geometry.Offset(w - 24f, y),
                                strokeWidth = 1.5f
                            )
                        }

                        // Draw 4 Corner alignment brackets
                        val bracketLen = 28f
                        val pad = 16f
                        val bracketColor = if (isProcessing) ElectricCyan else Color(0xFF64748B)

                        // Top-Left
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Offset(pad + bracketLen, pad), 3f)
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Offset(pad, pad + bracketLen), 3f)
                        // Top-Right
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(w - pad, pad), androidx.compose.ui.geometry.Offset(w - pad - bracketLen, pad), 3f)
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(w - pad, pad), androidx.compose.ui.geometry.Offset(w - pad, pad + bracketLen), 3f)
                        // Bottom-Left
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(pad, h - pad), androidx.compose.ui.geometry.Offset(pad + bracketLen, h - pad), 3f)
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(pad, h - pad), androidx.compose.ui.geometry.Offset(pad, h - pad - bracketLen), 3f)
                        // Bottom-Right
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(w - pad, h - pad), androidx.compose.ui.geometry.Offset(w - pad - bracketLen, h - pad), 3f)
                        drawLine(bracketColor, androidx.compose.ui.geometry.Offset(w - pad, h - pad), androidx.compose.ui.geometry.Offset(w - pad, h - pad - bracketLen), 3f)

                        // Laser scan line
                        if (isProcessing) {
                            val curLaserY = h * laserY
                            drawLine(
                                color = ElectricCyan,
                                start = androidx.compose.ui.geometry.Offset(pad, curLaserY),
                                end = androidx.compose.ui.geometry.Offset(w - pad, curLaserY),
                                strokeWidth = 3f
                            )
                        }
                    }

                    if (isProcessing) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = ElectricCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Extracting Notes...",
                                color = ElectricCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusText,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Capture Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { launchCameraSafely() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("take_photo_scanner_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_photo_scanner_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instant Demo Sheet Music Samples (Zero-setup instant trial)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Or try instant demo sheet music scores:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val rainBitmap = remember {
                        try {
                            val stream = context.assets.open("scores/rain_on_the_window.png")
                            android.graphics.BitmapFactory.decodeStream(stream)
                        } catch (_: Exception) {
                            generateSyntheticScore("Rain on the Window", 67)
                        }
                    }

                    val demoScores = listOf(
                        "Rain on the Window" to rainBitmap,
                        "Chopin Prelude" to generateSyntheticScore("Chopin Prelude", 64),
                        "Ode to Joy" to generateSyntheticScore("Ode to Joy", 72)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        demoScores.forEach { (name, syntheticBitmap) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceElevated)
                                    .clickable {
                                        onBitmapScanned?.invoke(syntheticBitmap)
                                        isProcessing = true
                                        statusText = "Analyzing $name..."
                                        scope.launch {
                                            val result = omrService.transcribeSheetMusic(syntheticBitmap, name)
                                            isProcessing = false
                                            result.onSuccess { song ->
                                                onSongScanned(song)
                                                onDismiss()
                                            }
                                        }
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates a synthetic sheet music bitmap for demonstration.
 */
private fun generateSyntheticScore(title: String, baseMidi: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Draw staves
    for (i in 0..4) {
        val y = 100f + i * 20f
        canvas.drawLine(40f, y, 360f, y, paint)
    }

    // Draw note heads
    val notePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    for (i in 0..7) {
        val x = 80f + i * 36f
        val y = 140f - (i % 4) * 10f
        canvas.drawCircle(x, y, 7f, notePaint)
        canvas.drawLine(x + 7f, y, x + 7f, y - 40f, paint)
    }

    return bitmap
}
