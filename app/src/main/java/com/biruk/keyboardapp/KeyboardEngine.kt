package com.biruk.keyboardapp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class KeyboardEngine {
    private data class Voice(
        val midiNote: Int,
        val frequency: Double,
        var phase: Double = 0.0,
        var releaseFramesLeft: Int = -1,
        val releaseFramesTotal: Int = 2205,
        val amplitude: Double = 0.18,
    )

    private val lock = Any()
    private val voices = linkedMapOf<Int, Voice>()
    private val pressCounts = linkedMapOf<Int, Int>()
    private var audioTrack: AudioTrack? = null
    private var worker: Thread? = null

    @Volatile
    var transposeSemitones: Int = 0

    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true

        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(BUFFER_SIZE * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }

        worker = Thread { renderLoop() }.apply {
            name = "BirukKeyboardSynth"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun noteOn(midiNote: Int): Int {
        val actualNote = (midiNote + transposeSemitones).coerceIn(0, 127)
        synchronized(lock) {
            val count = (pressCounts[actualNote] ?: 0) + 1
            pressCounts[actualNote] = count
            val voice = voices.getOrPut(actualNote) {
                Voice(
                    midiNote = actualNote,
                    frequency = midiToFrequency(actualNote),
                )
            }
            voice.releaseFramesLeft = -1
        }
        return actualNote
    }

    fun noteOff(actualNote: Int) {
        synchronized(lock) {
            val count = (pressCounts[actualNote] ?: 0) - 1
            if (count > 0) {
                pressCounts[actualNote] = count
                return
            }
            pressCounts.remove(actualNote)
            voices[actualNote]?.releaseFramesLeft = voices[actualNote]?.releaseFramesTotal ?: -1
        }
    }

    fun stopAll() {
        synchronized(lock) {
            voices.clear()
            pressCounts.clear()
        }
    }

    fun release() {
        running = false
        worker?.join(300)
        worker = null
        audioTrack?.run {
            try {
                pause()
            } catch (_: IllegalStateException) {
            }
            flush()
            release()
        }
        audioTrack = null
    }

    private fun renderLoop() {
        val track = audioTrack ?: return
        val buffer = ShortArray(BUFFER_SIZE)

        while (running) {
            synchronized(lock) {
                if (voices.isEmpty()) {
                    buffer.fill(0)
                    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                    return@synchronized
                }

                val completedNotes = mutableSetOf<Int>()
                for (i in buffer.indices) {
                    var mix = 0.0
                    val iterator = voices.values.iterator()
                    while (iterator.hasNext()) {
                        val voice = iterator.next()
                        val releaseFactor = if (voice.releaseFramesLeft >= 0) {
                            voice.releaseFramesLeft.toDouble() / voice.releaseFramesTotal.toDouble()
                        } else {
                            1.0
                        }
                        mix += sin(voice.phase) * voice.amplitude * releaseFactor
                        voice.phase += voice.phaseIncrement
                        if (voice.phase > TWO_PI) {
                            voice.phase -= TWO_PI
                        }
                        if (voice.releaseFramesLeft >= 0) {
                            voice.releaseFramesLeft -= 1
                            if (voice.releaseFramesLeft <= 0) {
                                completedNotes += voice.midiNote
                            }
                        }
                    }
                    buffer[i] = (mix.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                completedNotes.forEach { note ->
                    voices.remove(note)
                }
            }
            track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    private val Voice.phaseIncrement: Double
        get() = (TWO_PI * frequency) / SAMPLE_RATE

    private fun midiToFrequency(midiNote: Int): Double {
        return 440.0 * 2.0.pow((midiNote - 69) / 12.0)
    }

    private fun Double.pow(exponent: Double): Double = kotlin.math.pow(this, exponent)

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 1024
        private const val TWO_PI = (PI * 2.0)
    }
}
