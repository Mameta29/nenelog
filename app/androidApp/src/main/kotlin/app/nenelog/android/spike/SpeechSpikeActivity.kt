package app.nenelog.android.spike

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nenelog.android.AndroidAppGraph
import app.nenelog.android.AndroidNursingVoiceCommandHandler
import app.nenelog.domain.Side
import app.nenelog.domain.VoiceCommand
import app.nenelog.domain.VoicePhraseCandidateEvaluator
import app.nenelog.domain.VoiceRecognitionAttemptEvaluator
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Week 1 技術スパイク(docs/12)の Android 側ランナー。
 * 結果は research/spike-results.md に転記し、docs/16-risks.md R2 を更新する。
 */
class SpeechSpikeActivity : ComponentActivity() {

    private var inActivityLoop: RecognitionLoop? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val accuracyState = MutableStateFlow(AccuracyUiState())

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            SpikeLog.add("permissions: $result")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (
                        intent.getBooleanExtra(EXTRA_ACCURACY_TEST, false) ||
                        intent.getBooleanExtra(EXTRA_PHRASE_AB_TEST, false)
                    ) {
                        val state by accuracyState.collectAsState()
                        val profile = measurementProfile
                        AccuracyScreen(
                            state = state,
                            profile = profile,
                            onStartAttempt = ::startAccuracyAttempt,
                            onReset = ::resetAccuracyTest,
                        )
                    } else {
                        SpikeScreen(
                            onStartForeground = { locale, offline ->
                                inActivityLoop?.stop()
                                val commandHandler = AndroidNursingVoiceCommandHandler(
                                    AndroidAppGraph.nursing(this),
                                )
                                inActivityLoop = RecognitionLoop(
                                    this, locale, offline, speakBack = true, tag = "ACT",
                                    commandResponse = { transcript ->
                                        commandHandler.handle(
                                            transcript,
                                            locale,
                                            System.currentTimeMillis(),
                                        )
                                    },
                                ).also { it.start() }
                            },
                            onStopForeground = {
                                inActivityLoop?.stop()
                                inActivityLoop = null
                            },
                            onStartService = { locale, offline ->
                                startForegroundService(
                                    Intent(this, NursingVoiceService::class.java)
                                        .putExtra(NursingVoiceService.EXTRA_LOCALE, locale)
                                        .putExtra(NursingVoiceService.EXTRA_PREFER_OFFLINE, offline),
                                )
                            },
                            onStopService = {
                                startService(
                                    Intent(this, NursingVoiceService::class.java)
                                        .setAction(NursingVoiceService.ACTION_STOP_LISTENING),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        inActivityLoop?.stop()
        super.onDestroy()
    }

    private fun startAccuracyAttempt() {
        val state = accuracyState.value
        val profile = measurementProfile
        if (state.running || state.trialIndex >= profile.trials.size) return

        inActivityLoop?.stop()
        inActivityLoop = null
        accuracyState.value = state.copy(running = true, ready = false)
        mainHandler.postDelayed(
            {
                if (!accuracyState.value.running) return@postDelayed
                inActivityLoop = RecognitionLoop(
                    context = this,
                    locale = ACCURACY_LOCALE,
                    preferOffline = true,
                    speakBack = false,
                    tag = profile.logTag,
                    biasingStrings = profile.biasingStrings,
                    observer = object : RecognitionLoopObserver {
                        override fun onReady(attempt: Int) {
                            val current = accuracyState.value
                            if (current.running) {
                                accuracyState.value = current.copy(ready = true)
                            }
                        }

                        override fun onOutcome(outcome: RecognitionCycleOutcome) {
                            finishAccuracyAttempt(outcome)
                        }
                    },
                ).also { it.start() }
            },
            ACCURACY_RESTART_DELAY_MILLIS,
        )
    }

    private fun finishAccuracyAttempt(outcome: RecognitionCycleOutcome) {
        val state = accuracyState.value
        if (!state.running) return
        val profile = measurementProfile
        val trial = profile.trials.getOrNull(state.trialIndex) ?: return
        val lastPartialOnNoMatch = outcome.lastPartialText.takeIf {
            outcome.errorCode == SpeechRecognizer.ERROR_NO_MATCH
        }
        val productionEvaluation = trial.command?.let { expectedCommand ->
            VoiceRecognitionAttemptEvaluator.evaluate(
                finalCandidates = outcome.finalCandidates,
                lastPartialOnNoMatch = lastPartialOnNoMatch,
                locale = ACCURACY_LOCALE,
            ).let { evaluation -> evaluation to (evaluation.command == expectedCommand) }
        }
        val candidateEvaluation = trial.acceptedTranscripts.takeIf { it.isNotEmpty() }?.let {
            VoicePhraseCandidateEvaluator.evaluate(
                finalCandidates = outcome.finalCandidates,
                lastPartialOnNoMatch = lastPartialOnNoMatch,
                acceptedPhrases = it,
            )
        }
        val success = productionEvaluation?.second ?: (candidateEvaluation?.matchedPhrase != null)
        val selectedTranscript = productionEvaluation?.first?.selectedTranscript
            ?: candidateEvaluation?.selectedTranscript
        val heardText = selectedTranscript
            ?: outcome.finalCandidates.firstOrNull()
            ?: outcome.lastPartialText
            ?: "認識なし"
        val result = AccuracyTrialResult(
            number = state.trialIndex + 1,
            expectedLabel = trial.spokenLabel,
            heardText = heardText,
            success = success,
        )

        accuracyState.value = state.copy(
            trialIndex = state.trialIndex + 1,
            running = false,
            ready = false,
            successes = state.successes + if (success) 1 else 0,
            results = state.results + result,
        )
        SpikeLog.add(
            "[${profile.logTag}] ${result.number}/${profile.trials.size} " +
                "expected=${result.expectedLabel} heard=${result.heardText} " +
                "success=${result.success}",
        )
        inActivityLoop?.stop()
        inActivityLoop = null
    }

    private fun resetAccuracyTest() {
        inActivityLoop?.stop()
        inActivityLoop = null
        mainHandler.removeCallbacksAndMessages(null)
        accuracyState.value = AccuracyUiState()
        SpikeLog.add("[${measurementProfile.logTag}] measurement reset")
    }

    private fun requestPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    companion object {
        const val EXTRA_ACCURACY_TEST = "accuracy_test"
        const val EXTRA_PHRASE_AB_TEST = "phrase_ab_test"
        private const val ACCURACY_LOCALE = "ja-JP"
        private const val ACCURACY_RESTART_DELAY_MILLIS = 300L
    }

    private val measurementProfile: AccuracyMeasurementProfile
        get() = if (intent.getBooleanExtra(EXTRA_PHRASE_AB_TEST, false)) {
            PHRASE_AB_PROFILE
        } else {
            BASELINE_ACCURACY_PROFILE
        }
}

private data class AccuracyTrial(
    val spokenLabel: String,
    val command: VoiceCommand? = null,
    val acceptedTranscripts: Set<String> = emptySet(),
)

private data class AccuracyMeasurementProfile(
    val title: String,
    val subtitle: String,
    val logTag: String,
    val trials: List<AccuracyTrial>,
    val biasingStrings: List<String>? = null,
)

private data class AccuracyTrialResult(
    val number: Int,
    val expectedLabel: String,
    val heardText: String,
    val success: Boolean,
)

private data class AccuracyUiState(
    val trialIndex: Int = 0,
    val running: Boolean = false,
    val ready: Boolean = false,
    val successes: Int = 0,
    val results: List<AccuracyTrialResult> = emptyList(),
)

private val BASELINE_ACCURACY_PROFILE = AccuracyMeasurementProfile(
    title = "R7 音声認識精度測定",
    subtitle = "静音・通常声 / ja-JP / on-device",
    logTag = "R7",
    trials = buildList {
        repeat(10) {
            add(AccuracyTrial("みぎ、スタート", VoiceCommand.StartNursing(Side.RIGHT)))
            add(AccuracyTrial("ひだり、スタート", VoiceCommand.StartNursing(Side.LEFT)))
            add(AccuracyTrial("ストップ", VoiceCommand.StopNursing))
        }
    },
)

private val PHRASE_AB_PROFILE = AccuracyMeasurementProfile(
    title = "R7 開始語彙 A/B",
    subtitle = "診断専用 / 各候補5回 / 言い直しなし",
    logTag = "R7AB",
    trials = buildList {
        repeat(5) {
            add(
                AccuracyTrial(
                    spokenLabel = "みぎのおっぱい",
                    acceptedTranscripts = setOf("みぎのおっぱい", "右のおっぱい"),
                ),
            )
            add(
                AccuracyTrial(
                    spokenLabel = "ひだりのおっぱい",
                    acceptedTranscripts = setOf("ひだりのおっぱい", "左のおっぱい"),
                ),
            )
            add(
                AccuracyTrial(
                    spokenLabel = "みぎで授乳",
                    acceptedTranscripts = setOf("みぎで授乳", "右で授乳"),
                ),
            )
            add(
                AccuracyTrial(
                    spokenLabel = "ひだりで授乳",
                    acceptedTranscripts = setOf("ひだりで授乳", "左で授乳"),
                ),
            )
        }
    },
    biasingStrings = listOf(
        "みぎのおっぱい",
        "右のおっぱい",
        "ひだりのおっぱい",
        "左のおっぱい",
        "みぎで授乳",
        "右で授乳",
        "ひだりで授乳",
        "左で授乳",
    ),
)

@androidx.compose.runtime.Composable
private fun AccuracyScreen(
    state: AccuracyUiState,
    profile: AccuracyMeasurementProfile,
    onStartAttempt: () -> Unit,
    onReset: () -> Unit,
) {
    val total = profile.trials.size
    val nextTrial = profile.trials.getOrNull(state.trialIndex)
    val isComplete = state.trialIndex >= total

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(profile.title, style = MaterialTheme.typography.titleLarge)
        Text(
            profile.subtitle,
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
                        isComplete -> "測定完了"
                        state.running && !state.ready -> "準備中…"
                        state.running -> "今、1回だけ話してください"
                        else -> "次の発話"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    nextTrial?.spokenLabel ?: "全${profile.trials.size}回完了",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Button(
            onClick = onStartAttempt,
            enabled = !state.running && !isComplete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(top = 16.dp),
        ) {
            Text(if (state.running) "測定中…" else "この1回を開始")
        }

        Button(
            onClick = onReset,
            enabled = !state.running,
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

@androidx.compose.runtime.Composable
private fun SpikeScreen(
    onStartForeground: (locale: String, offline: Boolean) -> Unit,
    onStopForeground: () -> Unit,
    onStartService: (locale: String, offline: Boolean) -> Unit,
    onStopService: () -> Unit,
) {
    var japanese by remember { mutableStateOf(true) }
    var preferOffline by remember { mutableStateOf(true) }
    val lines by SpikeLog.lines.collectAsState()
    val locale = if (japanese) "ja-JP" else "en-US"

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Speech Spike (R2)", style = MaterialTheme.typography.titleLarge)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = japanese, onCheckedChange = { japanese = it })
            Text(if (japanese) "ja-JP" else "en-US")
            Switch(
                checked = preferOffline,
                onCheckedChange = { preferOffline = it },
                modifier = Modifier.padding(start = 16.dp),
            )
            Text("PREFER_OFFLINE")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onStartForeground(locale, preferOffline) }) { Text("画面オンで開始") }
            Button(onClick = onStopForeground) { Text("停止") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onStartService(locale, preferOffline) }) { Text("FGSで開始→画面オフ") }
            Button(onClick = onStopService) { Text("FGS停止") }
        }
        Button(onClick = { SpikeLog.clear() }) { Text("ログクリア") }

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            items(lines.asReversed()) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
