package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.model.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Royalty-free, copyright-free procedural audio engine for StudyOS.
 * Uses real-time PCM synthesis with Android AudioTrack to generate
 * soothing binaural beats, brown noise, gentle rain, and lo-fi ambient tones.
 */
class AmbientAudioSynthesizer {

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentSound = MutableStateFlow(SoundType.BINAURAL_ALPHA)
    val currentSound = _currentSound.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    val volume = _volume.asStateFlow()

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (_: Exception) {}
    }

    fun play(soundType: SoundType) {
        if (_isPlaying.value && _currentSound.value == soundType) {
            // Already playing this sound
            return
        }
        stop()
        _currentSound.value = soundType
        startSynthesis(soundType)
    }

    fun toggle(soundType: SoundType) {
        if (_isPlaying.value && _currentSound.value == soundType) {
            stop()
        } else {
            play(soundType)
        }
    }

    fun stop() {
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e("AudioSynth", "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
            _isPlaying.value = false
        }
    }

    private fun startSynthesis(soundType: SoundType) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(_volume.value)
            track.play()
            audioTrack = track
            _isPlaying.value = true

            synthesisJob = scope.launch {
                runSynthesizerLoop(track, soundType)
            }
        } catch (e: Exception) {
            Log.e("AudioSynth", "Failed to start AudioTrack", e)
            _isPlaying.value = false
        }
    }

    private fun runSynthesizerLoop(track: AudioTrack, soundType: SoundType) {
        val framesPerBuffer = bufferSize / 2 // stereo 16-bit = 4 bytes per frame
        val audioData = ShortArray(framesPerBuffer * 2)

        var phaseLeft = 0.0
        var phaseRight = 0.0
        var phaseLofi1 = 0.0
        var phaseLofi2 = 0.0
        var phaseLofi3 = 0.0

        var brownStateLeft = 0.0
        var brownStateRight = 0.0
        var rainFilterState = 0.0
        var lofiChordTimer = 0

        // Chord frequencies for LoFi ambient cycle (Dm9 -> G13 -> Cmaj9 -> Am7)
        val chords = listOf(
            doubleArrayOf(146.83, 174.61, 220.00, 261.63), // Dm7 (D3, F3, A3, C4)
            doubleArrayOf(130.81, 164.81, 196.00, 246.94), // Cmaj7 (C3, E3, G3, B3)
            doubleArrayOf(110.00, 164.81, 220.00, 261.63), // Am7 (A2, E3, A3, C4)
            doubleArrayOf(174.61, 220.00, 261.63, 329.63)  // Fmaj7 (F3, A3, C4, E4)
        )
        var chordIndex = 0

        while (scope.isActive) {
            when (soundType) {
                SoundType.BINAURAL_ALPHA -> {
                    // Alpha waves: 10 Hz difference (200 Hz Left, 210 Hz Right)
                    val freqL = 200.0
                    val freqR = 210.0
                    val stepL = 2.0 * PI * freqL / sampleRate
                    val stepR = 2.0 * PI * freqR / sampleRate

                    for (i in 0 until framesPerBuffer) {
                        val sampleL = (sin(phaseLeft) * 11000).toInt().toShort()
                        val sampleR = (sin(phaseRight) * 11000).toInt().toShort()
                        audioData[i * 2] = sampleL
                        audioData[i * 2 + 1] = sampleR

                        phaseLeft += stepL
                        phaseRight += stepR
                        if (phaseLeft > 2.0 * PI) phaseLeft -= 2.0 * PI
                        if (phaseRight > 2.0 * PI) phaseRight -= 2.0 * PI
                    }
                }

                SoundType.BINAURAL_THETA -> {
                    // Theta waves: 6 Hz difference (180 Hz Left, 186 Hz Right)
                    val freqL = 180.0
                    val freqR = 186.0
                    val stepL = 2.0 * PI * freqL / sampleRate
                    val stepR = 2.0 * PI * freqR / sampleRate

                    for (i in 0 until framesPerBuffer) {
                        val sampleL = (sin(phaseLeft) * 11000).toInt().toShort()
                        val sampleR = (sin(phaseRight) * 11000).toInt().toShort()
                        audioData[i * 2] = sampleL
                        audioData[i * 2 + 1] = sampleR

                        phaseLeft += stepL
                        phaseRight += stepR
                        if (phaseLeft > 2.0 * PI) phaseLeft -= 2.0 * PI
                        if (phaseRight > 2.0 * PI) phaseRight -= 2.0 * PI
                    }
                }

                SoundType.BROWN_NOISE -> {
                    // Integrated Gaussian noise with gentle decay for deep rumble
                    for (i in 0 until framesPerBuffer) {
                        val whiteL = (Random.nextDouble() * 2.0 - 1.0) * 1800.0
                        val whiteR = (Random.nextDouble() * 2.0 - 1.0) * 1800.0

                        brownStateLeft = (brownStateLeft * 0.96) + (whiteL * 0.08)
                        brownStateRight = (brownStateRight * 0.96) + (whiteR * 0.08)

                        audioData[i * 2] = brownStateLeft.coerceIn(-28000.0, 28000.0).toInt().toShort()
                        audioData[i * 2 + 1] = brownStateRight.coerceIn(-28000.0, 28000.0).toInt().toShort()
                    }
                }

                SoundType.RAIN_WINDOW -> {
                    // Gentle raindrop impulses + filtered noise
                    for (i in 0 until framesPerBuffer) {
                        val white = (Random.nextDouble() * 2.0 - 1.0) * 2200.0
                        // Resonant low-pass filter for window resonance
                        rainFilterState = rainFilterState * 0.92 + white * 0.08

                        // Occasional raindrop click/splatter
                        val dropImpulse = if (Random.nextFloat() < 0.0008f) {
                            (Random.nextDouble() * 14000.0 - 7000.0)
                        } else 0.0

                        val mixed = (rainFilterState * 3.2 + dropImpulse).coerceIn(-26000.0, 26000.0)
                        audioData[i * 2] = mixed.toInt().toShort()
                        audioData[i * 2 + 1] = (mixed * 0.95).toInt().toShort()
                    }
                }

                SoundType.LOFI_SYNTH -> {
                    // Soft evolving polyphonic ambient pads
                    lofiChordTimer += framesPerBuffer
                    if (lofiChordTimer > sampleRate * 5) { // change chord every 5 seconds
                        lofiChordTimer = 0
                        chordIndex = (chordIndex + 1) % chords.size
                    }

                    val currentChord = chords[chordIndex]
                    val step1 = 2.0 * PI * currentChord[0] / sampleRate
                    val step2 = 2.0 * PI * currentChord[1] / sampleRate
                    val step3 = 2.0 * PI * currentChord[2] / sampleRate

                    for (i in 0 until framesPerBuffer) {
                        val wave1 = sin(phaseLofi1) * 0.45
                        val wave2 = sin(phaseLofi2) * 0.35
                        val wave3 = sin(phaseLofi3) * 0.30

                        // Soft tape wow/flutter modulation
                        val flutter = 1.0 + 0.02 * sin(phaseLofi1 * 0.01)
                        val combined = ((wave1 + wave2 + wave3) * flutter * 9500).coerceIn(-30000.0, 30000.0)

                        audioData[i * 2] = combined.toInt().toShort()
                        audioData[i * 2 + 1] = (combined * 0.92).toInt().toShort()

                        phaseLofi1 += step1
                        phaseLofi2 += step2
                        phaseLofi3 += step3
                        if (phaseLofi1 > 2.0 * PI) phaseLofi1 -= 2.0 * PI
                        if (phaseLofi2 > 2.0 * PI) phaseLofi2 -= 2.0 * PI
                        if (phaseLofi3 > 2.0 * PI) phaseLofi3 -= 2.0 * PI
                    }
                }

                SoundType.COZY_LIBRARY -> {
                    // Soft pink noise with subtle warm page turn / ambience
                    for (i in 0 until framesPerBuffer) {
                        val white = (Random.nextDouble() * 2.0 - 1.0) * 1600.0
                        rainFilterState = rainFilterState * 0.94 + white * 0.06
                        val quietAir = (rainFilterState * 2.4).coerceIn(-20000.0, 20000.0)
                        audioData[i * 2] = quietAir.toInt().toShort()
                        audioData[i * 2 + 1] = quietAir.toInt().toShort()
                    }
                }
            }

            try {
                track.write(audioData, 0, audioData.size)
            } catch (e: Exception) {
                break
            }
        }
    }
}
