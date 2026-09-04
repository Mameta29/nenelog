package app.nenelog.android.spike

import android.Manifest
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import app.nenelog.domain.Side
import app.nenelog.domain.VoiceCommand
import app.nenelog.domain.VoiceCommandInterpreter
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService

/** R7診断専用。製品文法を変えず、Vosk固定文法をPixelで比較する。 */
class VoskCommandSpikeActivity : ComponentActivity() {

    private val uiState = MutableStateFlow(VoskUiState())
    private var model: Model? = null
    private var speechService: GainSpeechService? = null
    private var recognizer: Recognizer? = null
    private var lastPartialText: String? = null
    private var modelInitializationStarted = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                initializeModel()
            } else {
                uiState.value = uiState.value.copy(
                    loadingModel = false,
                    error = "マイク権限が必要です",
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by uiState.collectAsState()
                    VoskAccuracyScreen(
                        state = state,
                        onStartAttempt = ::startAttempt,
                        onReset = ::resetMeasurement,
                    )
                }
            }
        }
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onDestroy() {
        stopActiveRecognition()
        model?.close()
        model = null
        super.onDestroy()
    }

    private fun initializeModel() {
        if (modelInitializationStarted || model != null) return
        modelInitializationStarted = true
        uiState.value = uiState.value.copy(loadingModel = true, error = null)
        SpikeLog.add("[R7VOSK] model unpack start")
        StorageService.unpack(
            this,
            MODEL_ASSET_DIRECTORY,
            MODEL_STORAGE_DIRECTORY,
            { loadedModel ->
                model = loadedModel
                uiState.value = uiState.value.copy(loadingModel = false, modelReady = true)
                SpikeLog.add("[R7VOSK] model ready")
            },
            { exception ->
                uiState.value = uiState.value.copy(
                    loadingModel = false,
                    error = "モデル準備エラー: ${exception.message}",
                )
                SpikeLog.add("[R7VOSK] model error: ${exception.message}")
            },
        )
    }

    private fun startAttempt() {
        val state = uiState.value
        val loadedModel = model ?: return
        if (!state.modelReady || state.running || state.trialIndex >= VOSK_TRIALS.size) return

        stopActiveRecognition()
        lastPartialText = null
        try {
            val newRecognizer = Recognizer(loadedModel, SAMPLE_RATE, COMMAND_GRAMMAR)
            val newSpeechService = GainSpeechService(
                recognizer = newRecognizer,
                sampleRate = SAMPLE_RATE.toInt(),
                gain = INPUT_GAIN,
            )
            recognizer = newRecognizer
            speechService = newSpeechService
            uiState.value = state.copy(
                running = true,
                readyToSpeak = false,
                lastPartial = null,
                error = null,
            )
            SpikeLog.add(
                "[R7VOSK-GAIN] listen ${state.trialIndex + 1}/${VOSK_TRIALS.size} " +
                    "expected=${VOSK_TRIALS[state.trialIndex].spokenLabel}",
            )
            newSpeechService.startListening(recognitionListener, ATTEMPT_TIMEOUT_MILLIS)
            lifecycleScope.launch {
                delay(STARTUP_WARMUP_MILLIS)
                val current = uiState.value
                if (current.running && current.trialIndex == state.trialIndex) {
                    uiState.value = current.copy(readyToSpeak = true)
                    window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    SpikeLog.add("[R7VOSK-GAIN] ready ${state.trialIndex + 1}/${VOSK_TRIALS.size}")
                }
            }
        } catch (exception: IOException) {
            stopActiveRecognition()
            uiState.value = state.copy(error = "認識開始エラー: ${exception.message}")
            SpikeLog.add("[R7VOSK] start error: ${exception.message}")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String) {
            val text = hypothesisText(hypothesis, "partial") ?: return
            lastPartialText = text
            val state = uiState.value
            if (state.running) uiState.value = state.copy(lastPartial = text)
            SpikeLog.add("[R7VOSK-GAIN] partial: $text")
        }

        override fun onResult(hypothesis: String) {
            finishAttempt(hypothesisText(hypothesis, "text"))
        }

        override fun onFinalResult(hypothesis: String) {
            finishAttempt(hypothesisText(hypothesis, "text") ?: lastPartialText)
        }

        override fun onError(exception: Exception) {
            SpikeLog.add("[R7VOSK] recognition error: ${exception.message}")
            finishAttempt(null)
        }

        override fun onTimeout() {
            val finalText = recognizer?.finalResult
                ?.let { hypothesisText(it, "text") }
                ?: lastPartialText
            finishAttempt(finalText)
        }
    }

    private fun finishAttempt(transcript: String?) {
        val state = uiState.value
        if (!state.running) return
        val trial = VOSK_TRIALS.getOrNull(state.trialIndex) ?: return
        val command = transcript?.let { VoiceCommandInterpreter.interpret(it, "ja-JP") }
        val success = command == trial.command
        val heardText = transcript?.takeIf { it.isNotBlank() } ?: "認識なし"
        val result = VoskTrialResult(
            number = state.trialIndex + 1,
            expectedLabel = trial.spokenLabel,
            heardText = heardText,
            success = success,
        )

        uiState.value = state.copy(
            trialIndex = state.trialIndex + 1,
            running = false,
            readyToSpeak = false,
            successes = state.successes + if (success) 1 else 0,
            results = state.results + result,
            lastPartial = null,
        )
        SpikeLog.add(
            "[R7VOSK-GAIN] ${result.number}/${VOSK_TRIALS.size} " +
                "expected=${result.expectedLabel} heard=${result.heardText} " +
                "success=${result.success}",
        )
        stopActiveRecognition()
    }

    private fun resetMeasurement() {
        stopActiveRecognition()
        uiState.value = VoskUiState(modelReady = model != null, loadingModel = model == null)
        SpikeLog.add("[R7VOSK] measurement reset")
    }

    private fun stopActiveRecognition() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        recognizer?.close()
        recognizer = null
    }

    private fun hypothesisText(hypothesis: String, key: String): String? = runCatching {
        JSONObject(hypothesis).optString(key).trim().takeIf { it.isNotEmpty() }
    }.getOrNull()

    private companion object {
        const val MODEL_ASSET_DIRECTORY = "vosk-model-small-ja-0.22"
        const val MODEL_STORAGE_DIRECTORY = "vosk-model-ja"
        const val SAMPLE_RATE = 16_000.0f
        const val STARTUP_WARMUP_MILLIS = 700L
        const val ATTEMPT_TIMEOUT_MILLIS = 7_000
        const val INPUT_GAIN = 5.0f
        const val COMMAND_GRAMMAR =
            "[\"右 スタート\",\"左 スタート\",\"右\",\"左\",\"ストップ\",\"[unk]\"]"
    }
}

private data class VoskTrial(
    val spokenLabel: String,
    val command: VoiceCommand,
)

private data class VoskTrialResult(
    val number: Int,
    val expectedLabel: String,
    val heardText: String,
    val success: Boolean,
)

private data class VoskUiState(
    val modelReady: Boolean = false,
    val loadingModel: Boolean = true,
    val trialIndex: Int = 0,
    val running: Boolean = false,
    val readyToSpeak: Boolean = false,
    val successes: Int = 0,
    val results: List<VoskTrialResult> = emptyList(),
    val lastPartial: String? = null,
    val error: String? = null,
)

private val VOSK_TRIALS = buildList {
    repeat(5) {
        add(VoskTrial("みぎ、スタート", VoiceCommand.StartNursing(Side.RIGHT)))
        add(VoskTrial("ひだり、スタート", VoiceCommand.StartNursing(Side.LEFT)))
    }
}

@androidx.compose.runtime.Composable
private fun VoskAccuracyScreen(
    state: VoskUiState,
    onStartAttempt: () -> Unit,
    onReset: () -> Unit,
) {
    val total = VOSK_TRIALS.size
    val nextTrial = VOSK_TRIALS.getOrNull(state.trialIndex)
    val isComplete = state.trialIndex >= total

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("R7 Vosk小声入力補正", style = MaterialTheme.typography.titleLarge)
        Text(
            "50cm小声 / 入力5倍 / 振動後に1回だけ",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "進捗 ${state.trialIndex}/$total　成功 ${state.successes}/${state.trialIndex}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            ) {
                Text(
                    when {
                        state.error != null -> state.error
                        state.loadingModel -> "日本語モデル準備中…"
                        isComplete -> "測定完了"
                        state.running && !state.readyToSpeak -> "準備中。まだ話さないでください"
                        state.running -> "振動しました。今、1回だけ話してください"
                        else -> "次の発話"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    nextTrial?.spokenLabel ?: "全${VOSK_TRIALS.size}回完了",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                state.lastPartial?.let {
                    Text(
                        "途中候補: $it",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Button(
            onClick = onStartAttempt,
            enabled = state.modelReady && !state.running && !isComplete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(top = 16.dp),
        ) {
            Text(if (state.running) "測定中…" else "この1回を開始")
        }

        Button(
            onClick = onReset,
            enabled = state.modelReady && !state.running,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("最初からやり直す")
        }

        Text(
            "直近の結果",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            items(state.results.asReversed()) { result ->
                Text(
                    "${if (result.success) "✓" else "×"} ${result.number}. " +
                        "${result.expectedLabel} → ${result.heardText}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
