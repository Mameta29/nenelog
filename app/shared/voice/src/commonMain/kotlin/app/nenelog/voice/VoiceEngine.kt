package app.nenelog.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 音声レイヤーの共通抽象(docs/04)。
 * actual 実装は iosApp(Swift: SFSpeechRecognizer/SpeechTranscriber + AVSpeechSynthesizer)と
 * androidApp(SpeechRecognizer + TextToSpeech + FGS)が提供する。
 * エコー対策は「speak 中はマイク停止」で構造的に解決する(AEC に依存しない)。
 */
interface VoiceEngine {
    /** 部分/確定認識結果+信頼度 */
    val transcripts: Flow<TranscriptChunk>

    val state: StateFlow<VoiceEngineState>

    suspend fun startListening(mode: ListeningMode)

    suspend fun stopListening()

    suspend fun speak(text: String, profile: SpeechProfile)
}

enum class ListeningMode { PUSH_TO_TALK, SESSION }

enum class SpeechProfile { NORMAL, QUIET, HAPTICS_ONLY }

data class TranscriptChunk(
    val text: String,
    val isFinal: Boolean,
    /** 0.0..1.0。エンジンが返さない場合 null */
    val confidence: Float?,
    /** 例: "ja-JP", "en-US" */
    val locale: String,
)

sealed interface VoiceEngineState {
    data object Idle : VoiceEngineState
    data class Listening(val mode: ListeningMode) : VoiceEngineState
    data object Speaking : VoiceEngineState
    data class Error(val reason: VoiceErrorReason, val message: String? = null) : VoiceEngineState
}

enum class VoiceErrorReason {
    PERMISSION_DENIED,
    RECOGNIZER_UNAVAILABLE,
    OFFLINE_UNSUPPORTED,
    AUDIO_SESSION,
    UNKNOWN,
}
