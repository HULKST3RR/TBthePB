package com.example

import com.example.model.NoteUtils
import com.example.omr.SongRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ExampleUnitTest {

    @Test
    fun testMidiToNameConversions() {
        assertEquals("A0", NoteUtils.midiToName(21))
        assertEquals("C4", NoteUtils.midiToName(60)) // Middle C
        assertEquals("A4", NoteUtils.midiToName(69)) // Concert A 440Hz
        assertEquals("C8", NoteUtils.midiToName(108))
    }

    @Test
    fun testConcertAPitchFrequency() {
        val freqA4 = NoteUtils.midiToFrequency(69)
        assertEquals(440f, freqA4, 0.01f)

        val midiFromA4 = NoteUtils.frequencyToMidi(440f)
        assertEquals(69, midiFromA4.roundToInt())
    }

    @Test
    fun testWhiteKeyIdentification() {
        assertTrue(NoteUtils.isWhiteKey(60)) // C4
        assertFalse(NoteUtils.isWhiteKey(61)) // C#4
        assertTrue(NoteUtils.isWhiteKey(62)) // D4
        assertFalse(NoteUtils.isWhiteKey(63)) // D#4
        assertTrue(NoteUtils.isWhiteKey(64)) // E4
        assertTrue(NoteUtils.isWhiteKey(65)) // F4
        assertFalse(NoteUtils.isWhiteKey(66)) // F#4
        assertTrue(NoteUtils.isWhiteKey(67)) // G4
        assertFalse(NoteUtils.isWhiteKey(68)) // G#4
        assertTrue(NoteUtils.isWhiteKey(69)) // A4
        assertFalse(NoteUtils.isWhiteKey(70)) // A#4
        assertTrue(NoteUtils.isWhiteKey(71)) // B4
    }

    @Test
    fun testSongRepositoryRepertoire() {
        val songs = SongRepository.sampleSongs
        assertTrue(songs.isNotEmpty())

        val furElise = SongRepository.getSongById("fur_elise")
        assertEquals("Für Elise", furElise.title)
        assertTrue(furElise.notes.isNotEmpty())
    }
}
