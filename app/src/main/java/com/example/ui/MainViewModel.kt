package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PianoAudioEngine
import com.example.model.KeyVisualState
import com.example.model.NoteEvent
import com.example.model.NoteUtils
import com.example.model.PitchResult
import com.example.model.PracticeStats
import com.example.model.Song
import com.example.omr.OmrService
import com.example.omr.SongRepository
import com.example.pitch.PitchDetector
import com.example.ui.components.getWhiteKeyIndexForMidi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Main coordinator for KeySight 3D.
 * Bridges real-time audio synthesis, microphone pitch tracking, 3D keyboard dynamics,
 * sheet music OMR transcription, and the "Wait-for-Note" practice state machine.
 */
class MainViewModel : ViewModel() {

    val audioEngine = PianoAudioEngine()
    val pitchDetector = PitchDetector()
    val omrService = OmrService()

    private val _songs = MutableStateFlow(SongRepository.sampleSongs)
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow(SongRepository.sampleSongs.first())
    val currentSong: StateFlow<Song> = _currentSong.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPracticeMode = MutableStateFlow(false)
    val isPracticeMode: StateFlow<Boolean> = _isPracticeMode.asStateFlow()

    private val _isMetronomeActive = MutableStateFlow(false)
    val isMetronomeActive: StateFlow<Boolean> = _isMetronomeActive.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _viewportStartKey = MutableStateFlow(11) // Scaled to current piece
    val viewportStartKey: StateFlow<Int> = _viewportStartKey.asStateFlow()

    private val _visibleWhiteKeysCount = MutableStateFlow(24) // Framed to piece extents
    val visibleWhiteKeysCount: StateFlow<Int> = _visibleWhiteKeysCount.asStateFlow()

    private val _currentTargetNote = MutableStateFlow<NoteEvent?>(null)
    val currentTargetNote: StateFlow<NoteEvent?> = _currentTargetNote.asStateFlow()

    private val _isPitchMatched = MutableStateFlow(false)
    val isPitchMatched: StateFlow<Boolean> = _isPitchMatched.asStateFlow()

    private val _practiceStats = MutableStateFlow(PracticeStats())
    val practiceStats: StateFlow<PracticeStats> = _practiceStats.asStateFlow()

    private val _keyStates = MutableStateFlow(initializeKeyStates())
    val keyStates: StateFlow<List<KeyVisualState>> = _keyStates.asStateFlow()

    val pitchState: StateFlow<PitchResult> = pitchDetector.pitchState

    // UI Dialog & Viewer flags
    val showSongLibrary = MutableStateFlow(false)
    val showScanner = MutableStateFlow(false)
    val showSettings = MutableStateFlow(false)
    val showStats = MutableStateFlow(false)
    val showScoreViewer = MutableStateFlow(false)
    val isSplitScoreView = MutableStateFlow(false)

    private val _scannedScoreBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val scannedScoreBitmap: StateFlow<android.graphics.Bitmap?> = _scannedScoreBitmap.asStateFlow()

    fun setScannedScoreBitmap(bitmap: android.graphics.Bitmap?) {
        _scannedScoreBitmap.value = bitmap
    }

    fun toggleScoreViewer() {
        showScoreViewer.value = !showScoreViewer.value
    }

    fun toggleSplitScoreView() {
        isSplitScoreView.value = !isSplitScoreView.value
    }

    private var playbackLoopJob: Job? = null
    private var lastLoopTimeNs = System.nanoTime()
    private val soundingMidiNotes = mutableSetOf<Int>()
    private var lastEvaluatedNoteId: String? = null

    init {
        // Frame the keyboard range to the piece's lowest to highest note in scale
        framePieceExtents(_currentSong.value)

        // Collect pitch detection state for immediate visual feedback
        viewModelScope.launch {
            pitchDetector.pitchState.collect { pitch ->
                updateKeyVisualStatesWithPitch(pitch)
            }
        }

        startPlaybackLoop()
    }

    private fun initializeKeyStates(): List<KeyVisualState> {
        val list = mutableListOf<KeyVisualState>()
        for (midi in 21..108) {
            val keyIndex = midi - 21
            val isWhite = NoteUtils.isWhiteKey(midi)
            val name = NoteUtils.midiToName(midi)
            val octave = (midi / 12) - 1
            list.add(
                KeyVisualState(
                    keyIndex = keyIndex,
                    midiNumber = midi,
                    keyName = name,
                    isWhite = isWhite,
                    octave = octave
                )
            )
        }
        return list
    }

    private fun startPlaybackLoop() {
        playbackLoopJob?.cancel()
        playbackLoopJob = viewModelScope.launch {
            lastLoopTimeNs = System.nanoTime()

            while (isActive) {
                delay(20) // ~50 updates per second for smooth 3D rendering
                val nowNs = System.nanoTime()
                val deltaMs = ((nowNs - lastLoopTimeNs) / 1_000_000L).coerceIn(1L, 100L)
                lastLoopTimeNs = nowNs

                if (_isPlaying.value) {
                    processPlaybackTick(deltaMs)
                } else {
                    updateKeyVisualStates()
                }
            }
        }
    }

    private fun processPlaybackTick(deltaMs: Long) {
        val song = _currentSong.value
        val currentPos = _currentPositionMs.value
        val speed = _playbackSpeed.value

        if (_isPracticeMode.value) {
            // "Wait-for-Note" Practice Mode State Machine
            val nextNote = song.notes
                .filter { it.timestampMs >= currentPos - 150L }
                .minByOrNull { it.timestampMs }

            if (nextNote != null && nextNote.timestampMs <= currentPos + 250L) {
                _currentTargetNote.value = nextNote

                // Check if current microphone pitch matches target note
                val currentPitch = pitchDetector.pitchState.value
                val isMatched = currentPitch.isDetected &&
                        currentPitch.midiNote == nextNote.midiNumber &&
                        abs(currentPitch.centsOffset) <= pitchDetector.toleranceCents

                if (isMatched) {
                    _isPitchMatched.value = true
                    if (lastEvaluatedNoteId != nextNote.id) {
                        lastEvaluatedNoteId = nextNote.id
                        audioEngine.playSuccessChime()
                        recordPracticeHit()
                    }

                    // Advance past this note
                    _currentPositionMs.value = nextNote.endTimeMs + 10L
                    _isPitchMatched.value = false
                    _currentTargetNote.value = null
                } else {
                    // Suspend timeline progression until player plays the note!
                    // Keyboard is statically framed to piece extents so view does NOT jump
                    updateKeyVisualStates()
                    return
                }
            } else {
                _currentTargetNote.value = nextNote
                _currentPositionMs.value = currentPos + (deltaMs * speed).toLong()
            }
        } else {
            // Free Continuous Playback Mode
            val newPos = currentPos + (deltaMs * speed).toLong()

            // Check if end of song reached
            if (newPos > song.durationMs + 1000L) {
                _currentPositionMs.value = 0L
                _isPlaying.value = false
                audioEngine.stopAll()
                soundingMidiNotes.clear()
                return
            }

            _currentPositionMs.value = newPos

            // Metronome click on beat crossings
            val prevPos = currentPos
            if (_isMetronomeActive.value && song.bpm > 0) {
                val beatMs = (60_000.0 / song.bpm).toLong()
                val prevBeat = prevPos / beatMs
                val currentBeat = newPos / beatMs
                if (currentBeat > prevBeat) {
                    val isDownbeat = (currentBeat % 4L) == 0L
                    audioEngine.playMetronomeClick(isDownbeat)
                }
            }

            // Trigger acoustic audio synthesizer voices with exact note onset & release events
            val startedNotes = song.notes.filter { note ->
                note.timestampMs in (prevPos + 1)..newPos || (prevPos == 0L && note.timestampMs == 0L)
            }
            for (note in startedNotes) {
                audioEngine.noteOn(note.midiNumber, note.velocity)
                soundingMidiNotes.add(note.midiNumber)
            }

            val endedNotes = song.notes.filter { note ->
                note.endTimeMs in (prevPos + 1)..newPos
            }
            for (note in endedNotes) {
                val stillActive = song.notes.any { other ->
                    other.id != note.id && other.midiNumber == note.midiNumber && newPos in other.timestampMs..other.endTimeMs
                }
                if (!stillActive) {
                    audioEngine.noteOff(note.midiNumber)
                    soundingMidiNotes.remove(note.midiNumber)
                }
            }
        }

        updateKeyVisualStates()
    }

    private fun updateKeyVisualStates() {
        val currentPos = _currentPositionMs.value
        val song = _currentSong.value
        val target = _currentTargetNote.value
        val isMatched = _isPitchMatched.value

        val activeNotes = song.notes
            .filter { currentPos in it.timestampMs..it.endTimeMs }
        val activeNotesByMidi = activeNotes.associateBy { it.midiNumber }

        _keyStates.update { list ->
            list.map { key ->
                val isCue = target?.midiNumber == key.midiNumber
                val activeNote = activeNotesByMidi[key.midiNumber]
                val isPlayingNow = activeNote != null
                val isHit = isCue && isMatched
                val hand = activeNote?.hand ?: if (isCue) target?.hand else null

                key.copy(
                    isTargetCue = isCue,
                    isPlaying = isPlayingNow,
                    isHitSuccess = isHit,
                    activeHand = hand
                )
            }
        }
    }

    private fun updateKeyVisualStatesWithPitch(pitch: PitchResult) {
        if (!pitch.isDetected) {
            _keyStates.update { list ->
                list.map { if (it.isMicDetected) it.copy(isMicDetected = false) else it }
            }
            return
        }

        _keyStates.update { list ->
            list.map { key ->
                key.copy(isMicDetected = key.midiNumber == pitch.midiNote)
            }
        }
    }

    /**
     * Statically frames the 3D keyboard viewport from the lowest note of the piece
     * to the highest note in scale, preventing camera jumps during playback, and adapting
     * visibly to foldables (e.g. Samsung Galaxy Z Fold inner/outer screens).
     */
    fun framePieceExtents(song: Song, screenWidthDp: Int = 400) {
        val minWhite = getWhiteKeyIndexForMidi(song.lowestMidi).toInt()
        val maxWhite = getWhiteKeyIndexForMidi(song.highestMidi).toInt()
        val pieceSpan = (maxWhite - minWhite + 1).coerceAtLeast(1)

        val minVisible = when {
            screenWidthDp >= 800 -> 32
            screenWidthDp >= 600 -> 26
            screenWidthDp >= 400 -> 20
            else -> 16
        }
        val totalKeys = maxOf(pieceSpan + 2, minVisible).coerceIn(14, 52)
        val leftMargin = maxOf(1, (totalKeys - pieceSpan) / 2)
        val start = (minWhite - leftMargin).coerceIn(0, (52 - totalKeys).coerceAtLeast(0))
        _visibleWhiteKeysCount.value = totalKeys
        _viewportStartKey.value = start
    }

    fun togglePlayPause() {
        val willPlay = !_isPlaying.value
        _isPlaying.value = willPlay
        if (willPlay) {
            audioEngine.ensurePlaying()
            lastLoopTimeNs = System.nanoTime()
        } else {
            audioEngine.stopAll()
            soundingMidiNotes.clear()
        }
    }

    fun restartPlayback() {
        _currentPositionMs.value = 0L
        audioEngine.stopAll()
        soundingMidiNotes.clear()
        lastEvaluatedNoteId = null
        _currentTargetNote.value = null
        _isPitchMatched.value = false
        lastLoopTimeNs = System.nanoTime()
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs.coerceIn(0L, _currentSong.value.durationMs)
        audioEngine.stopAll()
        soundingMidiNotes.clear()
        lastEvaluatedNoteId = null
        lastLoopTimeNs = System.nanoTime()
    }

    fun setPracticeMode(enabled: Boolean) {
        _isPracticeMode.value = enabled
        if (!enabled) {
            _currentTargetNote.value = null
            _isPitchMatched.value = false
        }
    }

    fun toggleMetronome() {
        _isMetronomeActive.value = !_isMetronomeActive.value
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun setViewportStartKey(start: Int) {
        val maxStart = (52 - _visibleWhiteKeysCount.value).coerceAtLeast(0)
        _viewportStartKey.value = start.coerceIn(0, maxStart)
    }

    fun onUserKeyPress(midi: Int) {
        audioEngine.noteOn(midi, 0.85f)
        _keyStates.update { list ->
            list.map { if (it.midiNumber == midi) it.copy(isPlaying = true) else it }
        }

        // In Practice Mode, touching the target key directly also satisfies the practice cue!
        if (_isPracticeMode.value) {
            val target = _currentTargetNote.value
            if (target != null && target.midiNumber == midi) {
                _isPitchMatched.value = true
                audioEngine.playSuccessChime()
                recordPracticeHit()
                _currentPositionMs.value = target.endTimeMs + 10L
                _isPitchMatched.value = false
                _currentTargetNote.value = null
            }
        }
    }

    fun onUserKeyRelease(midi: Int) {
        audioEngine.noteOff(midi)
        _keyStates.update { list ->
            list.map { if (it.midiNumber == midi) it.copy(isPlaying = false) else it }
        }
    }

    fun selectSong(song: Song, screenWidthDp: Int = 400) {
        _currentSong.value = song
        framePieceExtents(song, screenWidthDp)
        restartPlayback()
    }

    fun addScannedSong(song: Song, screenWidthDp: Int = 400) {
        val updated = listOf(song) + _songs.value.filter { it.id != song.id }
        _songs.value = updated
        _currentSong.value = song
        framePieceExtents(song, screenWidthDp)
        restartPlayback()
    }

    private fun recordPracticeHit() {
        _practiceStats.update { current ->
            val newTotal = current.totalNotes + 1
            val newHit = current.notesHit + 1
            val newStreak = current.currentStreak + 1
            val best = maxOf(current.bestStreak, newStreak)
            current.copy(
                totalNotes = newTotal,
                notesHit = newHit,
                currentStreak = newStreak,
                bestStreak = best
            )
        }
    }

    fun resetPracticeStats() {
        _practiceStats.value = PracticeStats()
    }

    fun updateToleranceSettings(tolerance: Float, noiseGate: Float) {
        pitchDetector.toleranceCents = tolerance
        pitchDetector.noiseGateDb = noiseGate
    }

    override fun onCleared() {
        super.onCleared()
        playbackLoopJob?.cancel()
        audioEngine.release()
        pitchDetector.stopListening()
    }
}
