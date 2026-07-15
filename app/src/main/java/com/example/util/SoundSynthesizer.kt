package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sin

object SoundSynthesizer {
    private const val SAMPLE_RATE = 22050

    private suspend fun playTone(
        frequencies: List<Float>,
        durationMs: Int,
        type: String = "sine"
    ) = withContext(Dispatchers.Default) {
        try {
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val sample = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                var value = 0.0
                val t = i.toDouble() / SAMPLE_RATE
                for (f in frequencies) {
                    value += when (type) {
                        "square" -> if (sin(2.0 * Math.PI * f * t) >= 0) 0.4 else -0.4
                        else -> sin(2.0 * Math.PI * f * t) // sine
                    }
                }
                value /= frequencies.size
                sample[i] = (value * 16384).toInt().toShort() // Keep amplitude comfortable
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(sample, 0, numSamples)
            audioTrack.play()
            delay(durationMs.toLong() + 30)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun playClick() {
        playTone(listOf(1200f), 50, "sine")
    }

    suspend fun playReward() {
        playTone(listOf(523.25f), 80) // C5
        playTone(listOf(659.25f), 80) // E5
        playTone(listOf(783.99f), 80) // G5
        playTone(listOf(1046.50f), 150) // C6
    }

    suspend fun playLevelUp() {
        playTone(listOf(261.63f, 329.63f, 392.00f), 150) // C major chord
        playTone(listOf(349.23f, 440.00f, 523.25f), 150) // F major chord
        playTone(listOf(392.00f, 493.88f, 587.33f), 150) // G major chord
        playTone(listOf(523.25f, 659.25f, 783.99f, 1046.50f), 400) // High C major chord
    }

    suspend fun playWarning() {
        playTone(listOf(180f), 150, "square")
        delay(80)
        playTone(listOf(180f), 150, "square")
    }

    suspend fun playQuestComplete() {
        playTone(listOf(587.33f), 100) // D5
        playTone(listOf(739.99f), 100) // F#5
        playTone(listOf(880.00f), 100) // A5
        playTone(listOf(1174.66f), 250) // D6
    }
}
