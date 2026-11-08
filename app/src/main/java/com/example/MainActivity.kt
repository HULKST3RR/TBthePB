package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.ui.KeySightApp
import com.example.ui.MainViewModel
import com.example.ui.theme.KeySightTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeySightTheme {
                var isMicGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                var hasRequestedMicOnStart by remember { mutableStateOf(false) }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    isMicGranted = granted
                    if (granted) {
                        viewModel.pitchDetector.startListening()
                    }
                }

                LaunchedEffect(Unit) {
                    if (isMicGranted) {
                        viewModel.pitchDetector.startListening()
                    } else if (!hasRequestedMicOnStart) {
                        hasRequestedMicOnStart = true
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                KeySightApp(
                    viewModel = viewModel,
                    isMicPermissionGranted = isMicGranted,
                    onRequestMicPermission = {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.pitchDetector.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pitchDetector.stopListening()
        if (viewModel.isPlaying.value) {
            viewModel.togglePlayPause()
        }
    }
}
