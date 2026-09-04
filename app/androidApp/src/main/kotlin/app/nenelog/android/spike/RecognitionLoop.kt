package app.nenelog.android.spike

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import app.nenelog.domain.VoiceRecognitionFallback
import java.util.Locale

data class RecognitionReply(
    val spokenText: String,
    val endSessionAfterSpeaking: Boolean = false,
)

data class RecognitionCycleOutcome(
    val attempt: Int,
    val finalCandidates: List<String>,
    val lastPartialText: String?,
    val errorCode: Int? = null,
)

interface RecognitionLoopObserver {
    fun onReady(attempt: Int) = Unit
    fun onOutcome(outcome: RecognitionCycleOutcome) = Unit
}

/**
 * R2スパイクの心臓部: SpeechRecognizer の自動再起動ループ+TTS読み上げ。
 * docs/04 の方針どおり「speak 中は認識停止 → 終了後に再開」でエコーを構造的に回避する。
 * Activity(画面オン)と FGS(画面オフ)の両方から同じループを使い、挙動差を計測する。
 */
class RecognitionLoop(
    private val context: Context,
    private val locale: String = "ja-JP",
    private val preferOffline: Boolean = true,
    private val speakBack: Boolean = true,
    private val tag: String,
    private val commandResponse: ((String) -> RecognitionReply?)? = null,
    private val onSessionEnded: (() -> Unit)? = null,
    private val observer: RecognitionLoopObserver? = null,
    private val biasingStrings: List<String>? = null,
) : NursingRecognitionSession {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var running = false
    private var ttsReady = false
    private var restartCount = 0
    private var listenAttemptCount = 0
    private var activeListenAttempt = 0
    private var readyForSpeech = false
    private var speechStarted = false
    private var lastPartialText: String? = null
    private var pendingRestart: Runnable? = null
    private var endSessionAfterCurrentSpeech = false

    override fun start() {
        if (running) return
        running = true
        restartCount = 0
        listenAttemptCount = 0
        SpikeLog.add("[$tag] loop start locale=$locale preferOffline=$preferOffline")

        if (Build.VERSION.SDK_INT >= 31) {
            SpikeLog.add("[$tag] onDeviceRecognitionAvailable=" +
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
        }
        SpikeLog.add("[$tag] recognitionAvailable=" + SpeechRecognizer.isRecognitionAvailable(context))

        if (speakBack) {
            tts = TextToSpeech(context) { status ->
                SpikeLog.add("[$tag] TTS init status=$status")
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) tts?.language = Locale.forLanguageTag(locale)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        if (endSessionAfterCurrentSpeech) {
                            finishSession()
                        } else {
                            scheduleRestart("tts-error", delayMillis = 500)
                        }
                    }
                    override fun onDone(utteranceId: String?) {
                        if (endSessionAfterCurrentSpeech) {
                            finishSession()
                        } else {
                            // speak 終了後 300ms で認識再開(docs/04)
                            scheduleRestart("after-speak", delayMillis = 300)
                        }
                    }
                })
                handler.post { startListening() }
            }
        } else {
            handler.post { startListening() }
        }
    }

    override fun stop() {
        running = false
        pendingRestart?.let(handler::removeCallbacks)
        pendingRestart = null
        handler.post {
            recognizer?.destroy()
            recognizer = null
            tts?.shutdown()
            tts = null
            ttsReady = false
            endSessionAfterCurrentSpeech = false
            SpikeLog.add("[$tag] loop stopped (restarts=$restartCount)")
        }
    }

    private fun startListening() {
        if (!running) return
        val r = recognizer ?: createRecognizer().also { recognizer = it }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            if (Build.VERSION.SDK_INT >= 33) {
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_BIASING_STRINGS,
                    ArrayList(biasingStrings ?: commandBiasingStrings(locale)),
                )
            }
        }
        activeListenAttempt = ++listenAttemptCount
        readyForSpeech = false
        speechStarted = false
        lastPartialText = null
        SpikeLog.add("[$tag] listen request #$activeListenAttempt")
        r.startListening(intent)
    }

    private fun createRecognizer(): SpeechRecognizer {
        val r = if (Build.VERSION.SDK_INT >= 31 &&
            preferOffline && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpikeLog.add("[$tag] using createOnDeviceSpeechRecognizer")
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpikeLog.add("[$tag] using createSpeechRecognizer (default service)")
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                readyForSpeech = true
                SpikeLog.add("[$tag] ready #$activeListenAttempt")
                observer?.onReady(activeListenAttempt)
            }

            override fun onBeginningOfSpeech() {
                speechStarted = true
                SpikeLog.add("[$tag] speech begin #$activeListenAttempt")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                SpikeLog.add("[$tag] speech end #$activeListenAttempt")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    lastPartialText = text
                    SpikeLog.add("[$tag] partial: $text")
                }
            }

            override fun onResults(results: Bundle?) {
                val texts = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val best = texts.firstOrNull()
                SpikeLog.add("[$tag] FINAL: $best (conf=${scores?.firstOrNull()})")
                if (texts.size > 1) {
                    SpikeLog.add("[$tag] alternatives: ${texts.drop(1).joinToString(" | ")}")
                }
                observer?.onOutcome(
                    RecognitionCycleOutcome(
                        attempt = activeListenAttempt,
                        finalCandidates = texts,
                        lastPartialText = lastPartialText,
                    ),
                )
                val matched = if (commandResponse == null) {
                    best?.takeIf { it.isNotBlank() }?.let { it to RecognitionReply(it) }
                } else {
                    texts.firstNotNullOfOrNull { transcript ->
                        commandResponse.invoke(transcript)?.let { transcript to it }
                    }
                }
                val reply = matched?.second
                if (matched != null && matched.first != best) {
                    SpikeLog.add("[$tag] matched alternative: ${matched.first}")
                }
                if (reply != null) {
                    deliverReply(reply, restartReason = "after-results")
                } else {
                    if (!best.isNullOrBlank() && commandResponse != null) {
                        SpikeLog.add("[$tag] ignored: not a standalone Nenelog command")
                        scheduleRestart("after-ignored-result")
                    } else {
                        scheduleRestart("after-results")
                    }
                }
            }

            override fun onError(error: Int) {
                SpikeLog.add(
                    "[$tag] onError code=$error (${errorName(error)}) " +
                        "attempt=#$activeListenAttempt ready=$readyForSpeech speech=$speechStarted",
                )
                observer?.onOutcome(
                    RecognitionCycleOutcome(
                        attempt = activeListenAttempt,
                        finalCandidates = emptyList(),
                        lastPartialText = lastPartialText,
                        errorCode = error,
                    ),
                )
                if (error == SpeechRecognizer.ERROR_NO_MATCH && commandResponse != null) {
                    val fallbackText = VoiceRecognitionFallback.selectLastPartialOnNoMatch(
                        lastPartial = lastPartialText,
                        locale = locale,
                    )
                    val fallbackReply = fallbackText?.let(commandResponse::invoke)
                    if (fallbackText != null && fallbackReply != null) {
                        SpikeLog.add("[$tag] accepted final partial: $fallbackText")
                        deliverReply(fallbackReply, restartReason = "after-partial-fallback")
                        return
                    }
                }
                // サービス切断後は同じ SpeechRecognizer を再利用できない。
                if (error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED) {
                    recognizer?.destroy()
                    recognizer = null
                }
                val delayMillis = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> 350L
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                    -> 1_000L
                    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> 3_000L
                    else -> 750L
                }
                scheduleRestart("after-error-${errorName(error)}", delayMillis)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        return r
    }

    private fun deliverReply(reply: RecognitionReply, restartReason: String) {
        if (speakBack && ttsReady && reply.spokenText.isNotBlank()) {
            endSessionAfterCurrentSpeech = reply.endSessionAfterSpeaking
            SpikeLog.add("[$tag] response: ${reply.spokenText}")
            // 結果またはNO_MATCHが到着した時点で認識セッションは終了済み。cancel() は呼ばず、
            // TTS 完了コールバックからだけ次の認識を始める。
            val result = tts?.speak(
                reply.spokenText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "spike-${restartCount}",
            )
            if (result == TextToSpeech.ERROR) {
                if (endSessionAfterCurrentSpeech) {
                    finishSession()
                } else {
                    scheduleRestart("tts-start-error", delayMillis = 500)
                }
            }
        } else if (reply.endSessionAfterSpeaking) {
            finishSession()
        } else {
            scheduleRestart(restartReason)
        }
    }

    /**
     * RecognitionListener と TTS の複数コールバックが同時に到着しても、
     * 再開予約は常に1件だけにする。cancel() は新しい onError を発生させるため
     * 再開経路では呼ばない。
     */
    private fun scheduleRestart(reason: String, delayMillis: Long = 250) {
        handler.post {
            if (!running || pendingRestart != null) return@post
            val task = Runnable {
                pendingRestart = null
                if (!running) return@Runnable
                restartCount++
                SpikeLog.add("[$tag] restart #$restartCount ($reason)")
                startListening()
            }
            pendingRestart = task
            handler.postDelayed(task, delayMillis)
        }
    }

    private fun finishSession() {
        handler.post {
            if (!running) return@post
            endSessionAfterCurrentSpeech = false
            pendingRestart?.let(handler::removeCallbacks)
            pendingRestart = null
            SpikeLog.add("[$tag] nursing session completed")
            if (onSessionEnded != null) {
                onSessionEnded.invoke()
            } else {
                stop()
            }
        }
    }

    private fun errorName(code: Int) = when (code) {
        SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH_TIMEOUT"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "BUSY"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "PERMISSIONS"
        SpeechRecognizer.ERROR_CLIENT -> "CLIENT"
        SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
        SpeechRecognizer.ERROR_SERVER -> "SERVER"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "SERVER_DISCONNECTED"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "LANGUAGE_NOT_SUPPORTED"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "LANGUAGE_UNAVAILABLE"
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "CANNOT_CHECK_SUPPORT"
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS ->
            "CANNOT_LISTEN_TO_DOWNLOAD_EVENTS"
        else -> "code_$code"
    }

    private fun commandBiasingStrings(languageTag: String): List<String> =
        if (languageTag.startsWith("ja", ignoreCase = true)) {
            listOf("右", "右スタート", "左", "左スタート", "ストップ", "終わり", "ごちそうさま")
        } else {
            listOf("right", "right start", "start right", "left", "left start", "start left", "stop", "done")
        }

    private companion object {
        const val MAX_RESULTS = 5
    }
}
