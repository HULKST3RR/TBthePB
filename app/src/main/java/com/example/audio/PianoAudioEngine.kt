package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.NoteUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance polyphonic additive acoustic piano synthesizer.
 * Streams synthesized 16-bit PCM samples directly via an Android AudioTrack thread
 * to achieve sub-10ms latency without external binary dependencies.
 */
class PianoAudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE_FRAMES = 512
        private const val MAX_POLYPHONY = 16
    }

    private class Voice(
        val midi: Int,
        val frequency: Float,
        val velocity: Float,
        val startSample: Long
    ) {
        var phase: Double = 0.0
        var isReleased: Boolean = false
        var releaseSample: Long = 0L
        var ageSamples: Long = 0L

        // Inharmonicity factor typical of piano strings
        val bFactor: Double = 0.0001
    }

    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    private val activeVoices = ConcurrentHashMap<Int, Voice>()
    private var currentSampleIndex: Long = 0L

    init {
        start()
    }

    fun start() {
        if (isRunning.get()) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = if (minBufferSize > 0) maxOf(minBufferSize * 2, BUFFER_SIZE_FRAMES * 4) else 8192

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        isRunning.set(true)
        try {
            audioTrack?.play()
        } catch (_: Exception) {}

        renderThread = Thread({ renderLoop() }, "PianoAudioEngine-Renderer").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun ensurePlaying() {
        if (!isRunning.get() || audioTrack == null) {
            start()
        } else {
            try {
                if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack?.play()
                }
            } catch (_: Exception) {}
        }
    }

    fun noteOn(midi: Int, velocity: Float = 0.8f) {
        if (midi !in 21..108) return
        ensurePlaying()
        val freq = NoteUtils.midiToFrequency(midi)

        // Steal voice if max polyphony reached
        if (activeVoices.size >= MAX_POLYPHONY && !activeVoices.containsKey(midi)) {
            val oldestKey = activeVoices.minByOrNull { it.value.startSample }?.key
            if (oldestKey != null) {
                activeVoices.remove(oldestKey)
            }
        }

        activeVoices[midi] = Voice(
            midi = midi,
            frequency = freq,
            velocity = velocity.coerceIn(0.1f, 1.0f),
            startSample = currentSampleIndex
        )
    }

    fun noteOff(midi: Int) {
        val voice = activeVoices[midi]
        if (voice != null && !voice.isReleased) {
            voice.isReleased = true
            voice.releaseSample = voice.ageSamples
        }
    }

    fun stopAll() {
        activeVoices.clear()
    }

    fun playMetronomeClick(accent: Boolean = false) {
        val clickFreq = if (accent) 1200f else 800f
        val clickMidi = if (accent) 96 else 84
        noteOn(clickMidi, if (accent) 0.9f else 0.5f)
        Thread {
            try {
                Thread.sleep(35)
                noteOff(clickMidi)
            } catch (_: Exception) {}
        }.start()
    }

    fun playSuccessChime() {
        // Crisp golden chime chord (C5 + G5 + C6)
        noteOn(72, 0.6f)
        noteOn(79, 0.7f)
        noteOn(84, 0.8f)
        Thread {
            try {
                Thread.sleep(250)
                noteOff(72)
                noteOff(79)
                noteOff(84)
            } catch (_: Exception) {}
        }.start()
    }

    private fun renderLoop() {
        val buffer = ShortArray(BUFFER_SIZE_FRAMES)
        val twoPi = 2.0 * PI
        val samplePeriod = 1.0 / SAMPLE_RATE

        // Acoustic harmonic distribution weights
        val harmonicWeights = floatArrayOf(1.0f, 0.65f, 0.40f, 0.22f, 0.14f, 0.08f, 0.04f)

        while (isRunning.get()) {
            val voices = activeVoices.values.toList()

            for (i in 0 until BUFFER_SIZE_FRAMES) {
                var mixSample = 0.0

                for (voice in voices) {
                    val age = voice.ageSamples
                    val timeSec = age.toDouble() * samplePeriod

                    // Natural decay envelope: lower keys sustain longer (up to ~6s), higher keys decay quickly (~1s)
                    val decayRate = 1.2 + (voice.frequency / 350.0)
                    val baseEnvelope = exp(-decayRate * timeSec) * voice.velocity

                    // Damper release envelope
                    val envelope = if (voice.isReleased) {
                        val releaseSec = (age - voice.releaseSample).toDouble() * samplePeriod
                        val releaseDecay = exp(-18.0 * releaseSec) // Quick damper touch
                        baseEnvelope * releaseDecay
                    } else {
                        baseEnvelope
                    }

                    // Remove extinguished voices
                    if (envelope < 0.0005) {
                        activeVoices.remove(voice.midi, voice)
                        continue
                    }

                    // Hammer strike transient (first ~8ms)
                    val hammerTransient = if (timeSec < 0.008) {
                        val tFrac = 1.0 - (timeSec / 0.008)
                        (Math.random() * 2.0 - 1.0) * 0.15 * tFrac * voice.velocity
                    } else 0.0

                    // Sum harmonics with piano inharmonicity dispersion
                    var voiceSample = 0.0
                    for (h in harmonicWeights.indices) {
                        val harmonicNum = h + 1
                        val harmonicWeight = harmonicWeights[h] * exp(-0.35 * harmonicNum * timeSec) // High harmonics decay faster
                        val inharmonicMult = Math.sqrt(1.0 + voice.bFactor * harmonicNum * harmonicNum)
                        val hFreq = voice.frequency * harmonicNum * inharmonicMult
                        val hAngle = twoPi * hFreq * timeSec

                        voiceSample += sin(hAngle) * harmonicWeight
                    }

                    mixSample += (voiceSample * 0.55 * envelope) + hammerTransient
                    voice.ageSamples++
                }

                // Soft saturation limiter to prevent digital clipping
                val limited = Math.tanh(mixSample) * 32000.0
                buffer[i] = limited.toInt().coerceIn(-32767, 32767).toShort()
                currentSampleIndex++
            }

            audioTrack?.write(buffer, 0, BUFFER_SIZE_FRAMES)
        }
    }

    fun release() {
        isRunning.set(false)
        try {
            renderThread?.join(500)
        } catch (_: Exception) {}
        renderThread = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        activeVoices.clear()
    }
}
