package app.nenelog.android.spike

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.IOException
import java.util.Locale
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService

/** debug実機評価版では日本語L2をVoskへ差し替える。 */
class VoskNursingRecognitionSessionFactory : NursingRecognitionSessionFactory {
    override fun create(
        context: Context,
        locale: String,
        commandResponse: (String) -> RecognitionReply?,
        onSessionEnded: () -> Unit,
        onStateChanged: (String, String?) -> Unit,
    ): NursingRecognitionSession {
        val modelEntries = context.assets.list(MODEL_ASSET_DIRECTORY)
        check(!modelEntries.isNullOrEmpty()) { "Vosk model asset is not packaged" }
        return VoskNursingRecognitionSession(
            context = context.applicationContext,
            locale = locale,
            commandResponse = commandResponse,
            onSessionEnded = onSessionEnded,
            onStateChanged = onStateChanged,
        )
    }
}

private class VoskNursingRecognitionSession(
    private val context: Context,
    private val locale: String,
    private val commandResponse: (String) -> RecognitionReply?,
    private val onSessionEnded: () -> Unit,
    private val onStateChanged: (String, String?) -> Unit,
) : NursingRecognitionSession {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: GainSpeechService? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var lastPartial: String? = null
    private var cycleActive = false
    private var cycleNumber = 0
    private var pendingRestart: Runnable? = null
    private var fallback: RecognitionLoop? = null
    private var endSessionAfterSpeech = false

    override fun start() {
        handler.post {
            if (running) return@post
            running = true
            onStateChanged("waiting", null)
            SpikeLog.add("[FGS-VOSK] session start locale=$locale gain=$INPUT_GAIN")
            initializeTts()
            initializeModel()
        }
    }

    override fun stop() {
        running = false
        onStateChanged("waiting", null)
        pendingRestart?.let(handler::removeCallbacks)
        pendingRestart = null
        fallback?.stop()
        fallback = null
        cleanupRecognition()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        model?.close()
        model = null
        SpikeLog.add("[FGS-VOSK] session stopped")
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            handler.post {
                if (!running) return@post
                ttsReady = status == TextToSpeech.SUCCESS
                SpikeLog.add("[FGS-VOSK] TTS init status=$status")
                if (ttsReady) {
                    tts?.language = Locale.forLanguageTag(locale)
                    tts?.setOnUtteranceProgressListener(ttsListener)
                }
            }
        }
    }

    private fun initializeModel() {
        SpikeLog.add("[FGS-VOSK] model prepare start")
        StorageService.unpack(
            context,
            MODEL_ASSET_DIRECTORY,
            MODEL_STORAGE_DIRECTORY,
            { loadedModel ->
                handler.post {
                    if (!running) {
                        loadedModel.close()
                        return@post
                    }
                    model = loadedModel
                    SpikeLog.add("[FGS-VOSK] model ready")
                    onStateChanged("waiting", null)
                    startListening()
                }
            },
            { error ->
                handler.post {
                    if (running) startFallback("model error: ${error.message}")
                }
            },
        )
    }

    private fun startListening() {
        if (!running || fallback != null || cycleActive) return
        val loadedModel = model ?: return
        cleanupRecognition()
        lastPartial = null
        try {
            val newRecognizer = Recognizer(loadedModel, SAMPLE_RATE, COMMAND_GRAMMAR)
            val newSpeechService = GainSpeechService(
                recognizer = newRecognizer,
                sampleRate = SAMPLE_RATE.toInt(),
                gain = INPUT_GAIN,
            )
            recognizer = newRecognizer
            speechService = newSpeechService
            cycleActive = true
            val activeCycle = ++cycleNumber
            SpikeLog.add("[FGS-VOSK] listen #$activeCycle")
            onStateChanged("listening", null)
            newSpeechService.startListening(recognitionListener, LISTEN_TIMEOUT_MILLIS)
            handler.postDelayed(
                {
                    if (running && cycleActive && cycleNumber == activeCycle) {
                        SpikeLog.add("[FGS-VOSK] ready #$activeCycle")
                    }
                },
                STARTUP_WARMUP_MILLIS,
            )
        } catch (error: IOException) {
            cleanupRecognition()
            startFallback("audio start error: ${error.message}")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String) {
            val partial = hypothesisText(hypothesis, "partial") ?: return
            if (partial != lastPartial) {
                lastPartial = partial
                SpikeLog.add("[FGS-VOSK] partial: $partial")
            }
        }

        override fun onResult(hypothesis: String) {
            finishCycle(hypothesisText(hypothesis, "text") ?: lastPartial)
        }

        override fun onFinalResult(hypothesis: String) {
            finishCycle(hypothesisText(hypothesis, "text") ?: lastPartial)
        }

        override fun onError(exception: Exception) {
            SpikeLog.add("[FGS-VOSK] recognition error: ${exception.message}")
            finishCycle(null)
        }

        override fun onTimeout() {
            finishCycle(recognizer?.finalResult?.let { hypothesisText(it, "text") } ?: lastPartial)
        }
    }

    private fun finishCycle(transcript: String?) {
        if (!running || !cycleActive) return
        cycleActive = false
        val heard = transcript?.trim()?.takeIf { it.isNotEmpty() }
        SpikeLog.add("[FGS-VOSK] FINAL: ${heard ?: "認識なし"}")
        cleanupRecognition()

        val reply = heard?.let(commandResponse)
        if (reply == null) {
            if (heard != null && heard != "[unk]") onStateChanged("failure", heard)
            if (heard != null && heard != "[unk]") {
                SpikeLog.add("[FGS-VOSK] ignored: not a standalone Nenelog command")
            }
            scheduleRestart("after-no-command", NO_COMMAND_RESTART_MILLIS)
        } else {
            onStateChanged("recognized", heard)
            deliverReply(reply)
        }
    }

    private fun deliverReply(reply: RecognitionReply) {
        SpikeLog.add("[FGS-VOSK] response: ${reply.spokenText}")
        onStateChanged("responding", null)
        endSessionAfterSpeech = reply.endSessionAfterSpeaking
        if (!ttsReady || reply.spokenText.isBlank()) {
            if (endSessionAfterSpeech) finishSession() else scheduleRestart("tts-not-ready")
            return
        }
        val result = tts?.speak(
            reply.spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "fgs-vosk-$cycleNumber",
        )
        if (result == TextToSpeech.ERROR) {
            onStateChanged("failure", null)
            if (endSessionAfterSpeech) finishSession() else scheduleRestart("tts-start-error")
        }
    }

    private val ttsListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String?) {
            handler.post {
                onStateChanged("failure", null)
                if (endSessionAfterSpeech) finishSession() else scheduleRestart("tts-error")
            }
        }

        override fun onDone(utteranceId: String?) {
            handler.post {
                if (endSessionAfterSpeech) {
                    finishSession()
                } else {
                    scheduleRestart("after-speak", AFTER_SPEAK_RESTART_MILLIS)
                }
            }
        }
    }

    private fun scheduleRestart(reason: String, delayMillis: Long = DEFAULT_RESTART_MILLIS) {
        if (!running || pendingRestart != null || fallback != null) return
        val task = Runnable {
            pendingRestart = null
            if (!running || fallback != null) return@Runnable
            SpikeLog.add("[FGS-VOSK] restart ($reason)")
            onStateChanged("waiting", null)
            startListening()
        }
        pendingRestart = task
        handler.postDelayed(task, delayMillis)
    }

    private fun finishSession() {
        if (!running) return
        endSessionAfterSpeech = false
        SpikeLog.add("[FGS-VOSK] nursing session completed")
        onStateChanged("waiting", null)
        onSessionEnded()
    }

    private fun startFallback(reason: String) {
        if (!running || fallback != null) return
        SpikeLog.add("[FGS-VOSK] fallback to SpeechRecognizer: $reason")
        cleanupRecognition()
        model?.close()
        model = null
        tts?.shutdown()
        tts = null
        ttsReady = false
        fallback = RecognitionLoop(
            context = context,
            locale = locale,
            preferOffline = true,
            speakBack = true,
            tag = "FGS-FALLBACK",
            commandResponse = commandResponse,
            onSessionEnded = onSessionEnded,
            onStateChanged = onStateChanged,
        ).also { it.start() }
    }

    private fun cleanupRecognition() {
        cycleActive = false
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        recognizer?.close()
        recognizer = null
    }

    private fun hypothesisText(hypothesis: String, key: String): String? = runCatching {
        JSONObject(hypothesis).optString(key).trim().takeIf { it.isNotEmpty() }
    }.getOrNull()
}

private const val MODEL_ASSET_DIRECTORY = "vosk-model-small-ja-0.22"
private const val MODEL_STORAGE_DIRECTORY = "vosk-model-ja"
private const val SAMPLE_RATE = 16_000.0f
private const val INPUT_GAIN = 3.0f
private const val STARTUP_WARMUP_MILLIS = 700L
private const val LISTEN_TIMEOUT_MILLIS = 60_000
private const val NO_COMMAND_RESTART_MILLIS = 250L
private const val DEFAULT_RESTART_MILLIS = 500L
private const val AFTER_SPEAK_RESTART_MILLIS = 300L
private const val COMMAND_GRAMMAR =
    "[\"右 スタート\",\"左 スタート\",\"右\",\"左\",\"ストップ\",\"終わり\",\"ごちそうさま\",\"[unk]\"]"
