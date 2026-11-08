package com.example.pitch

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.model.NoteUtils
import com.example.model.PitchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Real-time microphone pitch detector utilizing the YIN algorithm
 * for high-accuracy fundamental frequency (F0) extraction.
 * Includes RMS noise gate, parabolic interpolation, and octave-aware MIDI mapping.
 */
class PitchDetector {

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 2048 // ~46.4ms window at 44.1kHz
        private const val YIN_THRESHOLD = 0.15f // Dip threshold for F0
        private const val SILENCE_THRESHOLD_DB = -42f // RMS noise gate in dBFS
        private const val MIN_FREQ = 55f   // A1
        private const val MAX_FREQ = 2100f // C7
    }

    private val _pitchState = MutableStateFlow(PitchResult())
    val pitchState: StateFlow<PitchResult> = _pitchState.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var noiseGateDb: Float = SILENCE_THRESHOLD_DB
    var toleranceCents: Float = 40f // User-adjustable pitch tolerance

    @SuppressLint("MissingPermission")
    fun startListening(): Boolean {
        if (recordingJob?.isActive == true) return true

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recordingBufferSize = maxOf(minBufferSize, BUFFER_SIZE * 2)

        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordingBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            recordingJob = scope.launch {
                processAudioLoop()
            }
            true
        } catch (e: Exception) {
            audioRecord = null
            false
        }
    }

    fun stopListening() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        _pitchState.value = PitchResult()
    }

    private suspend fun processAudioLoop() {
        val audioBuffer = ShortArray(BUFFER_SIZE)
        val floatBuffer = FloatArray(BUFFER_SIZE)

        while (scope.isActive) {
            val record = audioRecord ?: break
            val readCount = record.read(audioBuffer, 0, BUFFER_SIZE)
            if (readCount < BUFFER_SIZE) continue

            // 1. Calculate RMS energy and normalize to float [-1.0, 1.0]
            var sumSquare = 0.0
            for (i in 0 until BUFFER_SIZE) {
                val sample = audioBuffer[i]
                val normalized = sample / 32768f
                floatBuffer[i] = normalized
                sumSquare += (sample * sample).toDouble()
            }

            val rms = sqrt(sumSquare / BUFFER_SIZE)
            val rmsDb = if (rms > 0.0) (20.0 * log10(rms / 32768.0)).toFloat() else -100f

            // Noise gate: reject frames below threshold
            if (rmsDb < noiseGateDb) {
                _pitchState.value = PitchResult(
                    rmsDb = rmsDb,
                    isDetected = false
                )
                continue
            }

            // 2. Execute YIN pitch detection algorithm
            val detectedFreq = runYin(floatBuffer)

            if (detectedFreq in MIN_FREQ..MAX_FREQ) {
                val rawMidi = NoteUtils.frequencyToMidi(detectedFreq)
                val roundedMidi = rawMidi.roundToInt()
                val centsOffset = (rawMidi - roundedMidi) * 100f
                val noteName = NoteUtils.midiToName(roundedMidi)

                // Confidence estimate based on harmonic stability
                val confidence = ((1.0f - (Math.abs(centsOffset) / 50f)).coerceIn(0.2f, 1.0f))

                _pitchState.value = PitchResult(
                    frequencyHz = detectedFreq,
                    midiNote = roundedMidi,
                    noteName = noteName,
                    centsOffset = centsOffset,
                    confidence = confidence,
                    rmsDb = rmsDb,
                    isDetected = true
                )
            } else {
                _pitchState.value = PitchResult(
                    rmsDb = rmsDb,
                    isDetected = false
                )
            }
        }
    }

    /**
     * YIN Pitch Tracking Algorithm
     */
    private fun runYin(buffer: FloatArray): Float {
        val halfSize = BUFFER_SIZE / 2
        val diff = FloatArray(halfSize)

        // Step 1: Difference Function
        for (tau in 0 until halfSize) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 2: Cumulative Mean Normalized Difference
        val cmnd = FloatArray(halfSize)
        cmnd[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfSize) {
            runningSum += diff[tau]
            cmnd[tau] = if (runningSum > 0f) {
                diff[tau] / (runningSum / tau)
            } else {
                1f
            }
        }

        // Step 3: Absolute Thresholding
        val minTau = (SAMPLE_RATE / MAX_FREQ).toInt().coerceAtLeast(2)
        val maxTau = (SAMPLE_RATE / MIN_FREQ).toInt().coerceAtMost(halfSize - 2)

        var tauEstimate = -1
        for (tau in minTau until maxTau) {
            if (cmnd[tau] < YIN_THRESHOLD) {
                // Find local minimum below threshold
                var localTau = tau
                while (localTau + 1 < maxTau && cmnd[localTau + 1] < cmnd[localTau]) {
                    localTau++
                }
                tauEstimate = localTau
                break
            }
        }

        if (tauEstimate == -1) return -1f

        // Step 4: Parabolic Interpolation for sub-sample accuracy
        val s0 = cmnd[tauEstimate - 1]
        val s1 = cmnd[tauEstimate]
        val s2 = cmnd[tauEstimate + 1]
        val denominator = 2f * (s0 - 2f * s1 + s2)
        val delta = if (Math.abs(denominator) > 1e-6) (s0 - s2) / denominator else 0f
        val refinedTau = tauEstimate + delta

        return SAMPLE_RATE / refinedTau
    }
}
