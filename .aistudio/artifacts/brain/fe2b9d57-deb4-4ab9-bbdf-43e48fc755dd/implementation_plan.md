# KeySight 3D - Native Android 3D Piano & Sheet Music Learning Studio

KeySight 3D is a native Android application that transforms physical sheet music into interactive 3D piano performances. Combining real-time acoustic pitch detection, a perspective-projected 88-key physical piano simulation with falling note trails, low-latency polyphonic synthesis, and Gemini Vision Optical Music Recognition (OMR), KeySight 3D provides a "Wait-for-Note" interactive practice studio on Android.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during Phase 1 clarification:
> - **Keyboard Layout**: Full 88-key acoustic piano range (A0 to C8) with an interactive scrollable octave strip and dynamic camera viewport framing.
> - **Sheet Music Input**: Dual-mode workflow featuring on-device Camera sheet music capture powered by Gemini Vision OMR, paired with an offline curated classical library for instant zero-setup play.
> - **Practice Mode Evaluation**: Standard microphone pitch detection using YIN / Autocorrelation with configurable tolerance and ambient noise gating.

---

## 1. Overview & Core Concept

KeySight 3D bridges the gap between traditional sheet music and modern spatial music learning:
- **Spatial 3D Piano Experience**: A perspective-rendered 3D acoustic piano with physically accurate key dimensions, pivot rotations, multi-layer ambient occlusion, and falling Synthesia-style note ribbons.
- **Smart Sheet Music Capture (OMR)**: Snap physical sheet music or select an image to extract musical notation, clefs, tempos, and sequential note events into interactive songs via Gemini Vision.
- **Interactive "Wait-for-Note" Practice Loop**: Musicians play their physical acoustic piano or keyboard while the app listens via microphone, estimates fundamental frequency ($F_0$), highlights detected keys, and freezes song progression until the target note is correctly struck.
- **Polyphonic Piano Synthesizer**: Low-latency multi-voice acoustic modeling synthesizer outputting high-fidelity harmonics via direct PCM audio streaming.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Library & Song Selection**: Browse preloaded masterworks (*Für Elise*, *Rondo Alla Turca*, *Minuet in G*, *Moonlight Sonata*) or launch the **Sheet Music Camera Scanner**.
2. **Camera Scan & OMR Parsing**: Frame a page of sheet music with document alignment guides, capture the photo, and watch the AI OMR pipeline extract staves, key signatures, and note sequences with live preview.
3. **Song Playback & Study**: Press Play to watch the 3D piano keys depress in real time with synchronized falling note trails, adjust tempo from 0.5x to 1.5x, or drag the precision timeline scrubber.
4. **Interactive Practice Mode**: Switch to "Wait-for-Note" mode. The 3D piano highlights upcoming notes in neon cyan. The user plays on their physical piano; KeySight 3D tracks pitch in real time, displays an amber cue on the matched key, rewards correct hits with a golden burst, and advances the lesson.

```
┌────────────────────────────────────────────────────────┐
│  KeySight 3D              [BPM: 100] [Practice: ON] ⚙  │
├────────────────────────────────────────────────────────┤
│                                                        │
│   | |  | | |  | |  | | |      Falling Note Trails      │
│   | |  | | |  | |  | | |      (Synthesia Perspective)  │
│    v    v   v  v    v   v                              │
│ ┌────────────────────────────────────────────────────┐ │
│ │  C3   D3   E3   F3   G3   A3   B3   C4   D4   E4   │ │
│ │ [ ]  [ ]  [#]  [ ]  [#]  [#]  [ ]  [*]  [ ]  [ ]   │ │  <- 3D Piano Keys
│ └────────────────────────────────────────────────────┘ │     (* Cyan Cue)
├────────────────────────────────────────────────────────┤
│ [A0 . . . . . . . . . . . . [C3 - C5] . . . . . . . C8]│  <- Octave Nav Strip
├────────────────────────────────────────────────────────┤
│  Mic Pitch: C4 (261.6 Hz) | Conf: 94% | Accuracy: 98%  │
│  [⏮] [⏪]    [ ▶ PLAY / PAUSE ]    [⏩]   [📷 Scan Sheet] │
└────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Aesthetic Direction**: Luxury Obsidian & Cyber-Acoustic Concert Hall. Dark, rich piano-lacquer black background with brushed brass accents and crisp emissive indicators.
- **Color Palette**:
  - Background Base: `#0B0E14` (Deep Obsidian)
  - Surface Container: `#161B26` (Midnight Card Surface)
  - Key Natural (White): `#F8F9FA` with ivory bevels and subtle shadow gradients
  - Key Sharp (Black): `#1A1D24` with matte top highlights
  - Target Note Cue: `#00D4FF` (Electric Cyan)
  - Playback Active: `#10B981` (Emerald Green)
  - Mic Detected Pitch: `#F59E0B` (Warm Amber)
  - Correct Practice Hit: `#22C55E` (Vibrant Emerald Gold)
  - Mistake / Semitone Off: `#EF4444` (Coral Red)
- **Typography & Rhythm**:
  - High-contrast sans-serif hierarchy using Material 3 `Typography` with dedicated musical glyph and monospace pitch readouts (`C4 • 261.6 Hz`).

---

## 3. Key Product Decisions & Trade-Offs

- **3D Piano Rendering: Compose Perspective Canvas vs. Filament NDK**:
  - *Decision*: Implement a procedural 3D perspective projection engine directly in Compose `Canvas` with mathematical transformation matrices, dynamic key pivots, z-sorting, and lighting shaders.
  - *Rationale*: Eliminates heavy native C++ binary dependencies (`.so` packaging) that frequently fail or cause memory leaks in containerized emulators, while delivering 60 FPS fluid rendering, zero-lag recomposition, and seamless touch interaction.
- **Polyphonic Audio Synthesis: Pure Kotlin PCM AudioTrack Engine**:
  - *Decision*: Real-time multi-oscillator additive synthesizer with physical piano harmonic distribution (fundamental + odd/even harmonics + percussive hammer attack + exponential ADSR release).
  - *Rationale*: Guarantees instant sub-10ms audio feedback on all Android SDK levels without external SoundFont file missing errors.
- **Pitch Detection: High-Performance YIN / Autocorrelation Algorithm**:
  - *Decision*: Pure on-device DSP implementation running in a dedicated Coroutine thread over 2048-sample circular PCM buffers from `AudioRecord`.
  - *Rationale*: Sub-20ms latency, zero internet requirement, robust fundamental frequency extraction across octaves 2 through 6.
- **Sheet Music OMR: Gemini Vision Structured Output + Built-in Song Bank**:
  - *Decision*: High-accuracy multimodal Gemini analysis converting photographed sheet music images into structured JSON note events, combined with bundled classical pieces for immediate offline utility.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                             MainActivity                                │
│                                  │                                      │
│                      KeySightMainViewModel                              │
│       ┌──────────────────────────┼───────────────────────────┐          │
│       ▼                          ▼                           ▼          │
│  [AudioEngine]          [PitchDetector]              [SheetMusicRepo]   │
│  - Polyphonic Synth     - AudioRecord 44.1kHz        - Gemini Vision    │
│  - PCM AudioTrack       - YIN Algorithm ($F_0$)      - Classical Songs  │
│  - Playback Transport   - Noise Gate (-40dBFS)       - NoteEvent Model  │
│       │                          │                           │          │
│       └──────────────────────────┼───────────────────────────┘          │
│                                  ▼                                      │
│                        KeySightUiState Flow                             │
│                                  │                                      │
│  ┌───────────────────────────────┴───────────────────────────────┐      │
│  ▼                               ▼                               ▼      │
│ Piano3DCanvasView         PracticeController             ControlConsole │
│ - 88-Key Perspective      - Wait-for-Note Logic          - Play/Pause   │
│ - Falling Note Trails     - Tolerance Checking           - BPM Slider   │
│ - Key Pivot Dynamics      - Score & Accuracy             - Octave Nav   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Core Data Models
- `NoteEvent(id, noteName, midiNumber, timestampMs, durationMs, hand)`: Represents a discrete musical note.
- `Song(id, title, composer, bpm, difficulty, notes, timeSignature)`: Full score representation.
- `KeyVisualState(keyNumber, midi, isWhite, pivotAngle, targetCue, isPlaying, isMicDetected, isHit)`: Per-key rendering state.
- `PitchDetectionResult(frequencyHz, midiNote, noteName, confidence, rmsDb)`: Live audio input telemetry.

---

## 5. Execution Steps
1. **Application Identity & Permissions**: Update `metadata.json`, `strings.xml`, and `AndroidManifest.xml` with `RECORD_AUDIO` and `CAMERA` permissions.
2. **Audio Synthesis Engine**: Implement `PianoAudioEngine.kt` with polyphonic harmonic synthesis and zero-latency `AudioTrack` streaming.
3. **Real-Time Pitch Tracker**: Build `PitchDetector.kt` using the YIN autocorrelation algorithm with noise gating and MIDI mapping.
4. **Sheet Music & OMR Service**: Build `OmrService.kt` with Gemini Vision prompt parser and rich classical library (`SongRepository.kt`).
5. **Interactive 3D Piano Canvas**: Construct `Piano3DCanvas.kt` with mathematical perspective projection, falling Synthesia trails, dynamic key pivots, and lighting.
6. **Practice Coordinator & UI**: Implement `MainViewModel.kt` state machine and `KeySightApp.kt` Compose interface with octave navigation, playback controls, and practice statistics.
7. **Verification**: Compile and verify with `compile_applet`.
