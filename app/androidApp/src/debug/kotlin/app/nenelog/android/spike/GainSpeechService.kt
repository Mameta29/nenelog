package app.nenelog.android.spike

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener

/** R7診断専用。AudioRecordの生波形を計測し、Voskへ渡す前だけ増幅する。 */
internal class GainSpeechService(
    private val recognizer: Recognizer,
    private val sampleRate: Int,
    private val gain: Float,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bufferSamples = maxOf(
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ) / Short.SIZE_BYTES,
        sampleRate / 5,
    )
    private val recorder = AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSamples * Short.SIZE_BYTES,
    )

    @Volatile
    private var stopRequested = false
    private var worker: Thread? = null

    init {
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IOException("Failed to initialize AudioRecord")
        }
    }

    fun startListening(listener: RecognitionListener, timeoutMillis: Int): Boolean {
        if (worker != null) return false
        stopRequested = false
        worker = thread(name = "nenelog-vosk-gain") {
            recognize(listener, timeoutMillis)
        }
        return true
    }

    fun stop(): Boolean {
        val activeWorker = worker ?: return false
        stopRequested = true
        runCatching {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
        }
        if (activeWorker !== Thread.currentThread()) {
            runCatching { activeWorker.join(1_000) }
        }
        worker = null
        return true
    }

    fun shutdown() {
        stop()
        recorder.release()
    }

    private fun recognize(listener: RecognitionListener, timeoutMillis: Int) {
        val samples = ShortArray(bufferSamples)
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        var maxRawRms = 0.0
        var rawPeak = 0
        var clippedSamples = 0L
        var totalSamples = 0L
        var resultDelivered = false

        try {
            recorder.startRecording()
            while (!stopRequested && SystemClock.elapsedRealtime() < deadline) {
                val count = recorder.read(samples, 0, samples.size)
                if (count <= 0) continue

                var squareSum = 0.0
                repeat(count) { index ->
                    val raw = samples[index].toInt()
                    val absolute = if (raw == Short.MIN_VALUE.toInt()) 32_768 else kotlin.math.abs(raw)
                    if (absolute > rawPeak) rawPeak = absolute
                    squareSum += raw.toDouble() * raw.toDouble()

                    val amplified = (raw * gain).roundToInt()
                    if (amplified !in Short.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt()) {
                        clippedSamples++
                    }
                    samples[index] = amplified
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                totalSamples += count
                maxRawRms = maxOf(maxRawRms, sqrt(squareSum / count))

                if (recognizer.acceptWaveForm(samples, count)) {
                    val result = recognizer.result
                    logMetrics(maxRawRms, rawPeak, clippedSamples, totalSamples)
                    mainHandler.post { listener.onResult(result) }
                    resultDelivered = true
                    break
                } else {
                    val partial = recognizer.partialResult
                    mainHandler.post { listener.onPartialResult(partial) }
                }
            }

            if (!stopRequested && !resultDelivered) {
                val result = recognizer.finalResult
                logMetrics(maxRawRms, rawPeak, clippedSamples, totalSamples)
                mainHandler.post { listener.onFinalResult(result) }
            }
        } catch (exception: Exception) {
            if (!stopRequested) {
                mainHandler.post { listener.onError(exception) }
            }
        } finally {
            runCatching {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            }
        }
    }

    private fun logMetrics(
        maxRawRms: Double,
        rawPeak: Int,
        clippedSamples: Long,
        totalSamples: Long,
    ) {
        val clippedPermille = if (totalSamples == 0L) {
            0.0
        } else {
            clippedSamples.toDouble() * 1_000.0 / totalSamples
        }
        SpikeLog.add(
            "[R7VOSK-GAIN] audio gain=$gain maxRawRms=${maxRawRms.roundToInt()} " +
                "rawPeak=$rawPeak clippedPermille=${"%.2f".format(clippedPermille)}",
        )
    }
}
