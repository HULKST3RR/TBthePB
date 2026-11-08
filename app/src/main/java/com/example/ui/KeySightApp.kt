package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OctaveNavigator
import com.example.ui.components.Piano3DCanvas
import com.example.ui.components.PitchDisplayBar
import com.example.ui.components.PracticeStatsDialog
import com.example.ui.components.ScannedScoreViewer
import com.example.ui.components.ScannedScoreViewerDialog
import com.example.ui.components.SheetMusicScannerDialog
import com.example.ui.components.SongLibraryDialog
import com.example.ui.components.ToleranceSettingsDialog
import com.example.ui.components.TransportControls
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeySightApp(
    viewModel: MainViewModel,
    isMicPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isPracticeMode by viewModel.isPracticeMode.collectAsState()
    val isMetronomeActive by viewModel.isMetronomeActive.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val viewportStartKey by viewModel.viewportStartKey.collectAsState()
    val visibleWhiteKeysCount by viewModel.visibleWhiteKeysCount.collectAsState()
    val keyStates by viewModel.keyStates.collectAsState()
    val pitchResult by viewModel.pitchState.collectAsState()
    val currentTargetNote by viewModel.currentTargetNote.collectAsState()
    val isPitchMatched by viewModel.isPitchMatched.collectAsState()
    val practiceStats by viewModel.practiceStats.collectAsState()

    val showSongLibrary by viewModel.showSongLibrary.collectAsState()
    val showScanner by viewModel.showScanner.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val showStats by viewModel.showStats.collectAsState()
    val showScoreViewer by viewModel.showScoreViewer.collectAsState()
    val isSplitScoreView by viewModel.isSplitScoreView.collectAsState()
    val scannedScoreBitmap by viewModel.scannedScoreBitmap.collectAsState()

    // Automatically preload authentic score image on startup if none set
    LaunchedEffect(Unit) {
        if (viewModel.scannedScoreBitmap.value == null) {
            val bmp = try {
                val isStream = context.assets.open("scores/rain_on_the_window.png")
                BitmapFactory.decodeStream(isStream)
            } catch (_: Exception) {
                null
            }
            if (bmp != null) {
                viewModel.setScannedScoreBitmap(bmp)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthDp = maxWidth.value.toInt()
        val isFoldableWide = maxWidth >= 600.dp

        // Re-frame keyboard piece extents whenever current song or screen geometry changes
        LaunchedEffect(currentSong.id, screenWidthDp) {
            viewModel.framePieceExtents(currentSong, screenWidthDp)
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBlack,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Brand Logo
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricCyan.copy(alpha = 0.2f))
                                    .border(1.dp, ElectricCyan, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Piano,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Current Song Title & Composer clickable pill
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showSongLibrary.value = true }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                                    .testTag("top_song_selector_pill")
                            ) {
                                Text(
                                    text = currentSong.title,
                                    color = TextPrimary,
                                    fontSize = if (isFoldableWide) 16.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentSong.composer} • ${currentSong.keySignature} • ${currentSong.bpm} BPM",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // --- FOLDABLE SPECIAL: View Scanned Sheet Music Button ---
                            if (isFoldableWide) {
                                ElevatedButton(
                                    onClick = { viewModel.toggleScoreViewer() },
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = SurfaceElevated,
                                        contentColor = GoldAccent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("foldable_score_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Sheet Music Score",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Score Sheet",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Split Screen Toggle Button for Foldables
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSplitScoreView) GoldAccent.copy(alpha = 0.2f) else SurfaceElevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSplitScoreView) GoldAccent else BorderDark,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.toggleSplitScoreView() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("foldable_split_view_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VerticalSplit,
                                            contentDescription = "Split View",
                                            tint = if (isSplitScoreView) GoldAccent else TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSplitScoreView) "Split ON" else "Split View",
                                            color = if (isSplitScoreView) GoldAccent else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                // Mobile / Folded Screen Icon Button for Sheet Music
                                IconButton(
                                    onClick = { viewModel.toggleScoreViewer() },
                                    modifier = Modifier.testTag("mobile_score_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "View Scanned Score",
                                        tint = GoldAccent
                                    )
                                }
                            }

                            // Practice Stats Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceElevated)
                                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                                    .clickable { viewModel.showStats.value = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("top_stats_pill")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${practiceStats.accuracyPercentage}%",
                                        color = EmeraldGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Camera Sheet Music Scanner Action
                            IconButton(
                                onClick = { viewModel.showScanner.value = true },
                                modifier = Modifier.testTag("top_scanner_action_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan Sheet Music",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceDark
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Panoramic 88-Key Octave Navigator framed to piece extents
                    OctaveNavigator(
                        currentViewportStart = viewportStartKey,
                        visibleWhiteCount = visibleWhiteKeysCount,
                        keyStates = keyStates,
                        onViewportChange = { viewModel.setViewportStartKey(it) },
                        pieceLowestMidi = currentSong.lowestMidi,
                        pieceHighestMidi = currentSong.highestMidi
                    )

                    // Timeline Transport & Practice Controls
                    TransportControls(
                        isPlaying = isPlaying,
                        isPracticeMode = isPracticeMode,
                        isMetronomeActive = isMetronomeActive,
                        currentPositionMs = currentPositionMs,
                        totalDurationMs = currentSong.durationMs,
                        playbackSpeed = playbackSpeed,
                        bpm = currentSong.bpm,
                        onPlayPauseToggle = {
                            if (!isMicPermissionGranted && isPracticeMode) {
                                onRequestMicPermission()
                            }
                            viewModel.togglePlayPause()
                        },
                        onRestart = { viewModel.restartPlayback() },
                        onSeek = { viewModel.seekTo(it) },
                        onPracticeModeToggle = {
                            if (it && !isMicPermissionGranted) {
                                onRequestMicPermission()
                            }
                            viewModel.setPracticeMode(it)
                        },
                        onMetronomeToggle = { viewModel.toggleMetronome() },
                        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                        onOpenSongList = { viewModel.showSongLibrary.value = true },
                        onOpenScanner = { viewModel.showScanner.value = true }
                    )
                }
            }
        ) { innerPadding ->
            if (isFoldableWide && isSplitScoreView) {
                // --- FOLDABLE SPLIT SCREEN MODE: Sheet Music (Left) + 3D Piano (Right) ---
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Left Column: Interactive Scanned Sheet Music with Real-time Progress Bar & Tap-to-seek
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                    ) {
                        ScannedScoreViewer(
                            bitmap = scannedScoreBitmap,
                            song = currentSong,
                            currentPositionMs = currentPositionMs,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = { viewModel.togglePlayPause() },
                            onRestart = { viewModel.restartPlayback() },
                            onSeek = { viewModel.seekTo(it) },
                            onClose = { viewModel.toggleSplitScoreView() },
                            isEmbeddedSplitView = true
                        )
                    }

                    // Right Column: HUD telemetry & 3D Interactive Piano Keyboard
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
                    ) {
                        PitchDisplayBar(
                            pitchResult = pitchResult,
                            isPracticeMode = isPracticeMode,
                            currentTargetNote = currentTargetNote,
                            isCorrectPitchMatched = isPitchMatched,
                            isMicPermissionGranted = isMicPermissionGranted,
                            toleranceCents = viewModel.pitchDetector.toleranceCents,
                            onAdjustTolerance = { viewModel.showSettings.value = true }
                        )

                        PieceCompassAndLegendBar(
                            lowestNote = currentSong.lowestNoteName,
                            highestNote = currentSong.highestNoteName,
                            totalVisibleKeys = visibleWhiteKeysCount,
                            isFoldableWide = isFoldableWide,
                            onOpenScore = { viewModel.toggleScoreViewer() }
                        )

                        Piano3DCanvas(
                            keyStates = keyStates,
                            activeNotes = currentSong.notes,
                            currentPositionMs = currentPositionMs,
                            viewportStartKey = viewportStartKey,
                            visibleWhiteKeysCount = visibleWhiteKeysCount,
                            onKeyPress = { midi -> viewModel.onUserKeyPress(midi) },
                            onKeyRelease = { midi -> viewModel.onUserKeyRelease(midi) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("piano_3d_canvas_split")
                        )
                    }
                }
            } else {
                // --- STANDARD VIEW: HUD + 3D Piano Canvas ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Real-time Pitch Detection & Target Cue HUD
                    PitchDisplayBar(
                        pitchResult = pitchResult,
                        isPracticeMode = isPracticeMode,
                        currentTargetNote = currentTargetNote,
                        isCorrectPitchMatched = isPitchMatched,
                        isMicPermissionGranted = isMicPermissionGranted,
                        toleranceCents = viewModel.pitchDetector.toleranceCents,
                        onAdjustTolerance = { viewModel.showSettings.value = true }
                    )

                    // Piece Extents Compass & Hand Legend Ribbon with Score Button
                    PieceCompassAndLegendBar(
                        lowestNote = currentSong.lowestNoteName,
                        highestNote = currentSong.highestNoteName,
                        totalVisibleKeys = visibleWhiteKeysCount,
                        isFoldableWide = isFoldableWide,
                        onOpenScore = { viewModel.toggleScoreViewer() }
                    )

                    // 3D Perspective Interactive Piano Keyboard with Falling Notes
                    Piano3DCanvas(
                        keyStates = keyStates,
                        activeNotes = currentSong.notes,
                        currentPositionMs = currentPositionMs,
                        viewportStartKey = viewportStartKey,
                        visibleWhiteKeysCount = visibleWhiteKeysCount,
                        onKeyPress = { midi -> viewModel.onUserKeyPress(midi) },
                        onKeyRelease = { midi -> viewModel.onUserKeyRelease(midi) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("piano_3d_canvas")
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showScoreViewer) {
        ScannedScoreViewerDialog(
            bitmap = scannedScoreBitmap,
            song = currentSong,
            currentPositionMs = currentPositionMs,
            isPlaying = isPlaying,
            onPlayPauseToggle = { viewModel.togglePlayPause() },
            onRestart = { viewModel.restartPlayback() },
            onSeek = { viewModel.seekTo(it) },
            onDismiss = { viewModel.showScoreViewer.value = false }
        )
    }

    if (showSongLibrary) {
        SongLibraryDialog(
            songs = songs,
            selectedSongId = currentSong.id,
            onSongSelected = { viewModel.selectSong(it) },
            onDismiss = { viewModel.showSongLibrary.value = false }
        )
    }

    if (showScanner) {
        SheetMusicScannerDialog(
            omrService = viewModel.omrService,
            onSongScanned = { viewModel.addScannedSong(it) },
            onBitmapScanned = { bitmap -> viewModel.setScannedScoreBitmap(bitmap) },
            onDismiss = { viewModel.showScanner.value = false }
        )
    }

    if (showSettings) {
        ToleranceSettingsDialog(
            currentToleranceCents = viewModel.pitchDetector.toleranceCents,
            currentNoiseGateDb = viewModel.pitchDetector.noiseGateDb,
            onSaveSettings = { tol, gate -> viewModel.updateToleranceSettings(tol, gate) },
            onDismiss = { viewModel.showSettings.value = false }
        )
    }

    if (showStats) {
        PracticeStatsDialog(
            stats = practiceStats,
            onResetStats = { viewModel.resetPracticeStats() },
            onDismiss = { viewModel.showStats.value = false }
        )
    }
}

/**
 * Compact informational HUD strip displaying the piece's exact scale extents (lowest to highest note),
 * interactive Score Sheet opener button, and hand color legends (Right Hand Cyan vs Left Hand Magenta).
 */
@Composable
private fun PieceCompassAndLegendBar(
    lowestNote: String,
    highestNote: String,
    totalVisibleKeys: Int,
    isFoldableWide: Boolean,
    onOpenScore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090D14))
            .border(width = 0.5.dp, color = BorderDark)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Piece Scale Compass (Framed to piece extents)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Height,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Span: $lowestNote → $highestNote",
                color = TextPrimary,
                fontSize = if (isFoldableWide) 12.sp else 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "($totalVisibleKeys keys)",
                color = TextMuted,
                fontSize = 10.sp
            )

            // Sheet Music Score Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
                    .border(0.5.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .clickable { onOpenScore() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("hud_view_score_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "View Score",
                        tint = GoldAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Score",
                        color = GoldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Hand Differentiation Legend (Right Hand Cyan, Left Hand Magenta)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Right Hand Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RightHandColor)
                )
                Text(
                    text = "RH Melody",
                    color = RightHandColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Left Hand Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LeftHandColor)
                )
                Text(
                    text = "LH Bass",
                    color = LeftHandColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
