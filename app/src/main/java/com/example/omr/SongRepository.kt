package com.example.omr

import com.example.model.Hand
import com.example.model.NoteEvent
import com.example.model.NoteUtils
import com.example.model.Song

/**
 * Curated classical piano repertoire library with detailed polyphonic note events.
 */
object SongRepository {

    val sampleSongs: List<Song> by lazy {
        listOf(
            createRainOnTheWindow(),
            createFurElise(),
            createRondoAllaTurca(),
            createMinuetInG(),
            createGymnopedieNo1(),
            createChopinNocturne()
        )
    }

    fun getSongById(id: String): Song {
        return sampleSongs.find { it.id == id } ?: sampleSongs.first()
    }

    private fun note(
        midi: Int,
        startMs: Long,
        durationMs: Long,
        hand: Hand = Hand.RIGHT,
        velocity: Float = 0.8f
    ): NoteEvent {
        return NoteEvent(
            id = "${midi}_${startMs}",
            noteName = NoteUtils.midiToName(midi),
            midiNumber = midi,
            timestampMs = startMs,
            durationMs = durationMs,
            velocity = velocity,
            hand = hand
        )
    }

    /**
     * Ludwig van Beethoven - Für Elise (WoO 59 in A minor)
     */
    private fun createFurElise(): Song {
        val notes = mutableListOf<NoteEvent>()
        val beat = 380L // ~158 3/8 time

        // Theme A: E5, D#5, E5, D#5, E5, B4, D5, C5, A4
        var t = 0L
        notes.add(note(76, t, beat / 2)); t += beat / 2 // E5
        notes.add(note(75, t, beat / 2)); t += beat / 2 // D#5
        notes.add(note(76, t, beat / 2)); t += beat / 2 // E5
        notes.add(note(75, t, beat / 2)); t += beat / 2 // D#5
        notes.add(note(76, t, beat / 2)); t += beat / 2 // E5
        notes.add(note(71, t, beat / 2)); t += beat / 2 // B4
        notes.add(note(74, t, beat / 2)); t += beat / 2 // D5
        notes.add(note(72, t, beat / 2)); t += beat / 2 // C5
        notes.add(note(69, t, beat)); t += beat         // A4

        // Bass arpeggio: A2 - E3 - A3
        notes.add(note(45, t - beat, beat * 2, Hand.LEFT))
        notes.add(note(52, t - beat / 2, beat * 2, Hand.LEFT))

        // Right hand: C4, E4, A4, B4
        notes.add(note(60, t, beat / 2)); t += beat / 2 // C4
        notes.add(note(64, t, beat / 2)); t += beat / 2 // E4
        notes.add(note(69, t, beat / 2)); t += beat / 2 // A4
        notes.add(note(71, t, beat)); t += beat         // B4

        // Bass: E3 - G#3 - E4
        notes.add(note(40, t - beat, beat * 2, Hand.LEFT))

        // Right hand: E4, G#4, B4, C5
        notes.add(note(64, t, beat / 2)); t += beat / 2 // E4
        notes.add(note(68, t, beat / 2)); t += beat / 2 // G#4
        notes.add(note(71, t, beat / 2)); t += beat / 2 // B4
        notes.add(note(72, t, beat)); t += beat         // C5

        // Repeat theme fragment
        notes.add(note(76, t, beat / 2)); t += beat / 2 // E5
        notes.add(note(75, t, beat / 2)); t += beat / 2 // D#5
        notes.add(note(76, t, beat / 2)); t += beat / 2 // E5
        notes.add(note(75, t, beat / 2)); t += beat / 2 // D#5
        notes.add(note(76, t, beat / 2)); t += beat / 2 // E5
        notes.add(note(71, t, beat / 2)); t += beat / 2 // B4
        notes.add(note(74, t, beat / 2)); t += beat / 2 // D5
        notes.add(note(72, t, beat / 2)); t += beat / 2 // C5
        notes.add(note(69, t, beat * 2)); t += beat * 2 // A4

        return Song(
            id = "fur_elise",
            title = "Für Elise",
            composer = "Ludwig van Beethoven",
            bpm = 130,
            difficulty = "Beginner - Intermediate",
            keySignature = "A Minor",
            timeSignature = "3/8",
            notes = notes
        )
    }

    /**
     * Wolfgang Amadeus Mozart - Rondo Alla Turca (Turkish March)
     */
    private fun createRondoAllaTurca(): Song {
        val notes = mutableListOf<NoteEvent>()
        val step = 200L

        var t = 0L
        // B4, A4, G#4, A4, C5
        notes.add(note(71, t, step)); t += step
        notes.add(note(69, t, step)); t += step
        notes.add(note(68, t, step)); t += step
        notes.add(note(69, t, step)); t += step
        notes.add(note(72, t, step * 2)); t += step * 2

        // Left hand march chord (A minor)
        notes.add(note(45, t - step * 2, step * 3, Hand.LEFT))
        notes.add(note(57, t - step * 2, step * 3, Hand.LEFT))

        // D5, C5, B4, C5, E5
        notes.add(note(74, t, step)); t += step
        notes.add(note(72, t, step)); t += step
        notes.add(note(71, t, step)); t += step
        notes.add(note(72, t, step)); t += step
        notes.add(note(76, t, step * 2)); t += step * 2

        // F5, E5, D#5, E5, B5, A5, G#5, A5, B5
        notes.add(note(77, t, step)); t += step
        notes.add(note(76, t, step)); t += step
        notes.add(note(75, t, step)); t += step
        notes.add(note(76, t, step)); t += step
        notes.add(note(83, t, step)); t += step
        notes.add(note(81, t, step)); t += step
        notes.add(note(80, t, step)); t += step
        notes.add(note(81, t, step)); t += step
        notes.add(note(83, t, step * 2)); t += step * 2

        return Song(
            id = "rondo_alla_turca",
            title = "Rondo Alla Turca",
            composer = "W. A. Mozart",
            bpm = 126,
            difficulty = "Intermediate",
            keySignature = "A Minor / A Major",
            timeSignature = "2/4",
            notes = notes
        )
    }

    /**
     * J.S. Bach - Minuet in G major (BWV Anh. 114)
     */
    private fun createMinuetInG(): Song {
        val notes = mutableListOf<NoteEvent>()
        val quarter = 450L

        var t = 0L
        // D5, G4, A4, B4, C5, D5, G4, G4
        notes.add(note(74, t, quarter)); t += quarter
        notes.add(note(67, t, quarter / 2)); t += quarter / 2
        notes.add(note(69, t, quarter / 2)); t += quarter / 2
        notes.add(note(71, t, quarter / 2)); t += quarter / 2
        notes.add(note(72, t, quarter / 2)); t += quarter / 2
        notes.add(note(74, t, quarter)); t += quarter
        notes.add(note(67, t, quarter)); t += quarter
        notes.add(note(67, t, quarter)); t += quarter

        // Left hand G major chord
        notes.add(note(43, 0L, quarter * 3, Hand.LEFT))
        notes.add(note(55, quarter * 2, quarter * 2, Hand.LEFT))

        // E5, C5, D5, E5, F#5, G5, G4, G4
        notes.add(note(76, t, quarter)); t += quarter
        notes.add(note(72, t, quarter / 2)); t += quarter / 2
        notes.add(note(74, t, quarter / 2)); t += quarter / 2
        notes.add(note(76, t, quarter / 2)); t += quarter / 2
        notes.add(note(78, t, quarter / 2)); t += quarter / 2
        notes.add(note(79, t, quarter)); t += quarter
        notes.add(note(67, t, quarter)); t += quarter
        notes.add(note(67, t, quarter)); t += quarter

        return Song(
            id = "minuet_in_g",
            title = "Minuet in G major",
            composer = "J. S. Bach / Christian Petzold",
            bpm = 100,
            difficulty = "Beginner",
            keySignature = "G Major",
            timeSignature = "3/4",
            notes = notes
        )
    }

    /**
     * Erik Satie - Gymnopédie No. 1
     */
    private fun createGymnopedieNo1(): Song {
        val notes = mutableListOf<NoteEvent>()
        val bar = 1400L

        var t = 0L
        // Opening G major 7th atmosphere
        notes.add(note(43, t, bar, Hand.LEFT))        // G2
        notes.add(note(59, t + 400, bar - 400, Hand.LEFT)) // B3
        notes.add(note(62, t + 400, bar - 400, Hand.LEFT)) // D4
        notes.add(note(66, t + 400, bar - 400, Hand.LEFT)) // F#4
        t += bar

        // D major chord
        notes.add(note(38, t, bar, Hand.LEFT))        // D2
        notes.add(note(57, t + 400, bar - 400, Hand.LEFT)) // A3
        notes.add(note(62, t + 400, bar - 400, Hand.LEFT)) // D4
        notes.add(note(66, t + 400, bar - 400, Hand.LEFT)) // F#4
        t += bar

        // Melody enters: F#5 (78), A5 (81), G5 (79), F#5 (78), C#5 (73), B4 (71), C#5 (73), D5 (74)
        notes.add(note(78, t, bar / 2)); t += bar / 2
        notes.add(note(81, t, bar / 2)); t += bar / 2
        notes.add(note(79, t, bar / 2)); t += bar / 2
        notes.add(note(78, t, bar / 2)); t += bar / 2
        notes.add(note(73, t, bar)); t += bar
        notes.add(note(71, t, bar / 2)); t += bar / 2
        notes.add(note(73, t, bar / 2)); t += bar / 2
        notes.add(note(74, t, bar * 2)); t += bar * 2

        return Song(
            id = "gymnopedie_no1",
            title = "Gymnopédie No. 1",
            composer = "Erik Satie",
            bpm = 68,
            difficulty = "Intermediate",
            keySignature = "D Major",
            timeSignature = "3/4",
            notes = notes
        )
    }

    /**
     * Frédéric Chopin - Nocturne in E-flat major, Op. 9 No. 2
     */
    private fun createChopinNocturne(): Song {
        val notes = mutableListOf<NoteEvent>()
        val beat = 600L

        var t = 0L
        // Right hand romantic cantabile: Bb4, G5, F5, G5, Eb5, Bb4, C5, Bb4
        notes.add(note(70, t, beat / 2)); t += beat / 2
        notes.add(note(79, t, beat)); t += beat
        notes.add(note(77, t, beat / 3)); t += beat / 3
        notes.add(note(79, t, beat / 3)); t += beat / 3
        notes.add(note(75, t, beat * 2)); t += beat * 2

        // Left hand waltz accompaniment
        notes.add(note(39, 0L, beat * 3, Hand.LEFT)) // Eb2
        notes.add(note(58, beat, beat * 2, Hand.LEFT)) // Bb3
        notes.add(note(63, beat, beat * 2, Hand.LEFT)) // Eb4
        notes.add(note(67, beat, beat * 2, Hand.LEFT)) // G4

        notes.add(note(70, t, beat / 2)); t += beat / 2
        notes.add(note(72, t, beat / 2)); t += beat / 2
        notes.add(note(70, t, beat * 2)); t += beat * 2

        return Song(
            id = "chopin_nocturne",
            title = "Nocturne Op. 9 No. 2",
            composer = "Frédéric Chopin",
            bpm = 60,
            difficulty = "Advanced",
            keySignature = "Eb Major",
            timeSignature = "12/8",
            notes = notes
        )
    }

    /**
     * Mary Leaf - Rain on the Window (G minor, 4/4 Introspectively ca. 116 BPM)
     * Faithfully transcribed from physical sheet music.
     */
    /**
     * Mary Leaf - Rain on the Window (G minor, 4/4 Introspectively ca. 116 BPM)
     * Complete, faithful transcription of all 11 measures (entire Page 3).
     * Synchronized strictly to measure-based metric timestamps (no drift, no rushing, no overlaps).
     */
    fun createRainOnTheWindow(): Song {
        val notes = mutableListOf<NoteEvent>()
        val bpm = 116
        val quarter = 517L                 // 60,000 / 116 = 517ms
        val eighth = quarter / 2L          // 258ms
        val sixteenth = quarter / 4L       // 129ms
        val half = quarter * 2L            // 1034ms
        val dottedHalf = quarter * 3L      // 1551ms
        val whole = quarter * 4L           // 2068ms (1 measure of 4/4)

        fun mStart(m: Int): Long = (m - 1) * whole

        fun add(midi: Int, startMs: Long, durationMs: Long, hand: Hand, vel: Float = 0.8f) {
            notes.add(
                NoteEvent(
                    id = "${midi}_${startMs}_${hand.name.lowercase()}",
                    noteName = NoteUtils.midiToName(midi),
                    midiNumber = midi,
                    timestampMs = startMs,
                    durationMs = durationMs,
                    velocity = vel,
                    hand = hand
                )
            )
        }

        // --- SYSTEM 1: MEASURES 1, 2, 3 ---
        // Measure 1: (p, G minor, both hands in Treble Clef)
        val m1 = mStart(1)
        // LH: G4 (beat 1), D5 (beat 2), G5 (beat 3), D5 (beat 4)
        add(67, m1 + 0L, quarter, Hand.LEFT, 0.72f)
        add(74, m1 + quarter, quarter, Hand.LEFT, 0.70f)
        add(79, m1 + quarter * 2, quarter, Hand.LEFT, 0.75f)
        add(74, m1 + quarter * 3, quarter, Hand.LEFT, 0.70f)

        // RH: D5 (eighth), C5 (eighth), Bb4 (eighth), A4 (eighth), G4 (quarter tenuto), [rest], D5 (eighth pickup tied)
        add(74, m1 + 0L, eighth, Hand.RIGHT, 0.85f)
        add(72, m1 + eighth, eighth, Hand.RIGHT, 0.80f)
        add(70, m1 + quarter, eighth, Hand.RIGHT, 0.85f)
        add(69, m1 + quarter + eighth, eighth, Hand.RIGHT, 0.80f)
        add(67, m1 + quarter * 2, quarter, Hand.RIGHT, 0.90f)
        // Pickup on "and" of 4 tied across into Measure 2:
        add(74, m1 + quarter * 3 + eighth, eighth + dottedHalf, Hand.RIGHT, 0.85f)

        // Measure 2:
        val m2 = mStart(2)
        // LH: F#4, D5, F#5, D5
        add(66, m2 + 0L, quarter, Hand.LEFT, 0.72f)
        add(74, m2 + quarter, quarter, Hand.LEFT, 0.70f)
        add(78, m2 + quarter * 2, quarter, Hand.LEFT, 0.75f)
        add(74, m2 + quarter * 3, quarter, Hand.LEFT, 0.70f)

        // RH: Tied D5 holds through beat 3; beat 4 has D5 (eighth), C5 (eighth)
        add(74, m2 + quarter * 3, eighth, Hand.RIGHT, 0.82f)
        add(72, m2 + quarter * 3 + eighth, eighth, Hand.RIGHT, 0.80f)

        // Measure 3:
        val m3 = mStart(3)
        // LH: G4, D5, G5, D5
        add(67, m3 + 0L, quarter, Hand.LEFT, 0.72f)
        add(74, m3 + quarter, quarter, Hand.LEFT, 0.70f)
        add(79, m3 + quarter * 2, quarter, Hand.LEFT, 0.75f)
        add(74, m3 + quarter * 3, quarter, Hand.LEFT, 0.70f)

        // RH: D5, C5, Bb4, A4, G4 (tenuto), [rest], D5 (pickup tied)
        add(74, m3 + 0L, eighth, Hand.RIGHT, 0.85f)
        add(72, m3 + eighth, eighth, Hand.RIGHT, 0.80f)
        add(70, m3 + quarter, eighth, Hand.RIGHT, 0.85f)
        add(69, m3 + quarter + eighth, eighth, Hand.RIGHT, 0.80f)
        add(67, m3 + quarter * 2, quarter, Hand.RIGHT, 0.90f)
        add(74, m3 + quarter * 3 + eighth, eighth + dottedHalf, Hand.RIGHT, 0.85f)

        // --- SYSTEM 2: MEASURES 4, 5, 6 ---
        // Measure 4: [4]
        val m4 = mStart(4)
        // LH: F#4, D5, F#5, D5
        add(66, m4 + 0L, quarter, Hand.LEFT, 0.72f)
        add(74, m4 + quarter, quarter, Hand.LEFT, 0.70f)
        add(78, m4 + quarter * 2, quarter, Hand.LEFT, 0.75f)
        add(74, m4 + quarter * 3, quarter, Hand.LEFT, 0.70f)

        // RH: Tied D5 holds through beat 3; beat 4 has D5 (eighth), C5 (eighth)
        add(74, m4 + quarter * 3, eighth, Hand.RIGHT, 0.82f)
        add(72, m4 + quarter * 3 + eighth, eighth, Hand.RIGHT, 0.80f)

        // Measure 5: (LH switches to Bass Clef, triplets)
        val m5 = mStart(5)
        val trip = quarter / 3L
        // LH Bass Clef: D3 bass anchor + triplets
        add(50, m5 + 0L, whole, Hand.LEFT, 0.78f)
        add(62, m5 + quarter, trip, Hand.LEFT, 0.70f)
        add(66, m5 + quarter + trip, trip, Hand.LEFT, 0.70f)
        add(69, m5 + quarter + trip * 2, trip, Hand.LEFT, 0.70f)

        add(62, m5 + quarter * 2, trip, Hand.LEFT, 0.70f)
        add(66, m5 + quarter * 2 + trip, trip, Hand.LEFT, 0.70f)
        add(69, m5 + quarter * 2 + trip * 2, trip, Hand.LEFT, 0.70f)

        // RH: Beat 1 chord D4 + Bb4; beat 2 triplets D5-Bb4-G4; beat 3 A4 (half)
        add(62, m5 + 0L, quarter, Hand.RIGHT, 0.80f)
        add(70, m5 + 0L, quarter, Hand.RIGHT, 0.85f)
        add(74, m5 + quarter, trip, Hand.RIGHT, 0.85f)
        add(70, m5 + quarter + trip, trip, Hand.RIGHT, 0.80f)
        add(67, m5 + quarter + trip * 2, trip, Hand.RIGHT, 0.80f)
        add(69, m5 + quarter * 2, half, Hand.RIGHT, 0.85f)

        // Measure 6:
        val m6 = mStart(6)
        // LH: D3 bass anchor + A3
        add(50, m6 + 0L, whole, Hand.LEFT, 0.78f)
        add(57, m6 + quarter, half, Hand.LEFT, 0.70f)
        // RH: G4 (quarter), A4 (quarter), D4, F#4, A4, D5 (eighths)
        add(67, m6 + 0L, quarter, Hand.RIGHT, 0.82f)
        add(69, m6 + quarter, quarter, Hand.RIGHT, 0.85f)
        add(62, m6 + quarter * 2, eighth, Hand.RIGHT, 0.80f)
        add(66, m6 + quarter * 2 + eighth, eighth, Hand.RIGHT, 0.82f)
        add(69, m6 + quarter * 3, eighth, Hand.RIGHT, 0.85f)
        add(74, m6 + quarter * 3 + eighth, eighth, Hand.RIGHT, 0.88f)

        // --- SYSTEM 3: MEASURES 7, 8 ---
        // Measure 7: [7] (Grand upward arpeggio build-up)
        val m7 = mStart(7)
        // LH: Low G2 octave with G3
        add(43, m7 + 0L, whole, Hand.LEFT, 0.88f)
        add(55, m7 + 0L, whole, Hand.LEFT, 0.80f)
        // RH: D4 (quarter), G4 (eighth), Bb4 (eighth)
        add(62, m7 + 0L, quarter, Hand.RIGHT, 0.82f)
        add(67, m7 + quarter, eighth, Hand.RIGHT, 0.84f)
        add(70, m7 + quarter + eighth, eighth, Hand.RIGHT, 0.86f)
        // Beat 3: four 16th notes climbing to high G5
        add(74, m7 + quarter * 2, sixteenth, Hand.RIGHT, 0.88f)
        add(75, m7 + quarter * 2 + sixteenth, sixteenth, Hand.RIGHT, 0.88f)
        add(77, m7 + quarter * 2 + sixteenth * 2, sixteenth, Hand.RIGHT, 0.90f)
        add(78, m7 + quarter * 2 + sixteenth * 3, sixteenth, Hand.RIGHT, 0.92f)
        // Beat 4: High G5 apex
        add(79, m7 + quarter * 3, quarter, Hand.RIGHT, 0.98f)

        // Measure 8: (poco rit., cascading descent)
        val m8 = mStart(8)
        // LH: D3 bass support
        add(50, m8 + 0L, whole, Hand.LEFT, 0.75f)
        // RH: G5 held on beat 1, then cascading descent
        add(79, m8 + 0L, quarter, Hand.RIGHT, 0.92f)
        val ritStep = (quarter * 3L) / 8L
        val runNotes = intArrayOf(78, 77, 75, 74, 72, 70, 69, 67)
        for (i in runNotes.indices) {
            add(runNotes[i], m8 + quarter + (i * ritStep), ritStep, Hand.RIGHT, 0.82f)
        }

        // --- SYSTEM 4: MEASURES 9, 10, 11 ---
        // Measure 9: [9] ("a tempo", mf, main theme reprise)
        val m9 = mStart(9)
        // LH returns to Treble clef:
        add(67, m9 + 0L, quarter, Hand.LEFT, 0.78f)
        add(74, m9 + quarter, quarter, Hand.LEFT, 0.75f)
        add(79, m9 + quarter * 2, quarter, Hand.LEFT, 0.80f)
        add(74, m9 + quarter * 3, quarter, Hand.LEFT, 0.75f)

        // RH: D5, C5, Bb4, A4, G4 (tenuto), [rest], D5 (pickup tied)
        add(74, m9 + 0L, eighth, Hand.RIGHT, 0.90f)
        add(72, m9 + eighth, eighth, Hand.RIGHT, 0.85f)
        add(70, m9 + quarter, eighth, Hand.RIGHT, 0.88f)
        add(69, m9 + quarter + eighth, eighth, Hand.RIGHT, 0.85f)
        add(67, m9 + quarter * 2, quarter, Hand.RIGHT, 0.92f)
        add(74, m9 + quarter * 3 + eighth, eighth + dottedHalf, Hand.RIGHT, 0.88f)

        // Measure 10:
        val m10 = mStart(10)
        // LH: F#4, D5, F#5, D5
        add(66, m10 + 0L, quarter, Hand.LEFT, 0.78f)
        add(74, m10 + quarter, quarter, Hand.LEFT, 0.75f)
        add(78, m10 + quarter * 2, quarter, Hand.LEFT, 0.80f)
        add(74, m10 + quarter * 3, quarter, Hand.LEFT, 0.75f)

        // RH: Tied D5 holds through beat 3; beat 4 has D5 (eighth), C5 (eighth)
        add(74, m10 + quarter * 3, eighth, Hand.RIGHT, 0.85f)
        add(72, m10 + quarter * 3 + eighth, eighth, Hand.RIGHT, 0.82f)

        // Measure 11: (Final resolution cadence of Page 3)
        val m11 = mStart(11)
        // LH: Low G2 + G3 bass anchor + G4 - D5
        add(43, m11 + 0L, whole + quarter, Hand.LEFT, 0.85f)
        add(55, m11 + 0L, whole + quarter, Hand.LEFT, 0.80f)
        add(67, m11 + quarter, half, Hand.LEFT, 0.75f)
        add(74, m11 + quarter * 2, half, Hand.LEFT, 0.72f)

        // RH: Final G minor chord ringing peacefully: G4 + Bb4 + D5
        add(67, m11 + 0L, whole + quarter, Hand.RIGHT, 0.90f)
        add(70, m11 + 0L, whole + quarter, Hand.RIGHT, 0.85f)
        add(74, m11 + 0L, whole + quarter, Hand.RIGHT, 0.88f)

        return Song(
            id = "rain_on_the_window",
            title = "Rain on the Window",
            composer = "Mary Leaf",
            bpm = 116,
            difficulty = "Intermediate (Page 3)",
            keySignature = "G Minor",
            timeSignature = "4/4",
            notes = notes
        )
    }
}
