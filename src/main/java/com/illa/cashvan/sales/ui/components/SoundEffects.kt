package com.illa.cashvan.sales.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp

private const val SAMPLE_RATE = 22_050
private const val VOLUME = 0.12

/** (frequency Hz, start ms, length s): the PWA's WebAudio victory fanfare. */
private val FANFARE = listOf(
    Triple(523.25, 0, 0.2),
    Triple(659.25, 100, 0.2),
    Triple(783.99, 200, 0.2),
    Triple(1046.5, 300, 0.4),
    Triple(783.99, 450, 0.15),
    Triple(1046.5, 600, 0.6)
)

/**
 * Plays the short triangle-wave fanfare used for order and commission celebrations. Synthesised on the fly,
 * with a 10 ms attack and exponential decay per note, so no audio asset is needed.
 */
suspend fun playVictoryFanfare() = withContext(Dispatchers.Default) {
    val totalSeconds = FANFARE.maxOf { (_, start, length) -> start / 1000.0 + length }
    val samples = ShortArray((totalSeconds * SAMPLE_RATE).toInt())
    val mix = DoubleArray(samples.size)
    FANFARE.forEach { (frequency, startMs, length) ->
        val offset = startMs * SAMPLE_RATE / 1000
        val count = (length * SAMPLE_RATE).toInt()
        val attack = 0.01 * SAMPLE_RATE
        for (i in 0 until count) {
            val index = offset + i
            if (index >= mix.size) break
            val phase = (i * frequency / SAMPLE_RATE) % 1.0
            val triangle = 4 * abs(phase - 0.5) - 1
            val envelope = if (i < attack) i / attack else exp(-5.0 * (i - attack) / count)
            mix[index] += triangle * envelope * VOLUME
        }
    }
    mix.forEachIndexed { i, value -> samples[i] = (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }

    runCatching {
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        kotlinx.coroutines.delay((totalSeconds * 1000).toLong() + 100)
        track.release()
    }
}
