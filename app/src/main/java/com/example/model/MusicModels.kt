package com.example.model

/**
 * Represents a musical note event in a song timeline.
 */
data class NoteEvent(
    val id: String,
    val noteName: String, // e.g. "C4", "G#4", "Eb5"
    val midiNumber: Int,  // MIDI standard 21 (A0) to 108 (C8)
    val timestampMs: Long,
    val durationMs: Long,
    val velocity: Float = 0.8f,
    val hand: Hand = Hand.RIGHT
) {
    val endTimeMs: Long get() = timestampMs + durationMs
}

enum class Hand {
    LEFT, RIGHT
}

/**
 * Complete musical piece for playback and practice.
 */
data class Song(
    val id: String,
    val title: String,
    val composer: String,
    val bpm: Int = 120,
    val difficulty: String = "Medium",
    val keySignature: String = "C Major",
    val timeSignature: String = "4/4",
    val notes: List<NoteEvent> = emptyList(),
    val durationMs: Long = notes.maxOfOrNull { it.endTimeMs } ?: 0L
) {
    val lowestMidi: Int get() = notes.minOfOrNull { it.midiNumber } ?: 60
    val highestMidi: Int get() = notes.maxOfOrNull { it.midiNumber } ?: 72
    val lowestNoteName: String get() = NoteUtils.midiToName(lowestMidi)
    val highestNoteName: String get() = NoteUtils.midiToName(highestMidi)
    val midiSpan: Int get() = (highestMidi - lowestMidi + 1).coerceAtLeast(12)
}

/**
 * Rendering state for individual piano keys in the 3D canvas.
 */
data class KeyVisualState(
    val keyIndex: Int,       // 0 to 87
    val midiNumber: Int,     // 21 to 108
    val keyName: String,     // e.g. "C4", "A0"
    val isWhite: Boolean,
    val octave: Int,
    val pivotDeg: Float = 0f, // 0 to -3.5 degrees
    val isTargetCue: Boolean = false,
    val isPlaying: Boolean = false,
    val isMicDetected: Boolean = false,
    val isHitSuccess: Boolean = false,
    val activeHand: Hand? = null
)

/**
 * Real-time microphone pitch estimation telemetry.
 */
data class PitchResult(
    val frequencyHz: Float = 0f,
    val midiNote: Int = 0,
    val noteName: String = "--",
    val centsOffset: Float = 0f,
    val confidence: Float = 0f, // 0.0 to 1.0
    val rmsDb: Float = -100f,
    val isDetected: Boolean = false
)

/**
 * Statistics tracked during Wait-for-Note practice.
 */
data class PracticeStats(
    val totalNotes: Int = 0,
    val notesHit: Int = 0,
    val notesMissed: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    val accuracyPercentage: Int
        get() = if (totalNotes == 0) 100 else ((notesHit.toFloat() / totalNotes.toFloat()) * 100).toInt()
}

/**
 * Musical note utility helpers.
 */
object NoteUtils {
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun midiToName(midi: Int): String {
        if (midi !in 21..108) return "--"
        val noteIndex = midi % 12
        val octave = (midi / 12) - 1
        return "${NOTE_NAMES[noteIndex]}$octave"
    }

    fun midiToFrequency(midi: Int): Float {
        return (440.0 * Math.pow(2.0, (midi - 69).toDouble() / 12.0)).toFloat()
    }

    fun frequencyToMidi(freq: Float): Float {
        if (freq <= 0) return 0f
        return (69.0 + 12.0 * (Math.log(freq.toDouble() / 440.0) / Math.log(2.0))).toFloat()
    }

    fun isWhiteKey(midi: Int): Boolean {
        val noteInOctave = midi % 12
        return noteInOctave in intArrayOf(0, 2, 4, 5, 7, 9, 11) // C, D, E, F, G, A, B
    }
}
