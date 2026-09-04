package app.nenelog.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.nenelog.data.CareSummary
import app.nenelog.data.NursingCommandResult
import app.nenelog.domain.Event
import app.nenelog.ui.theme.ThemePreference

enum class AppDestination { HOME, JOURNAL, SUMMARY, SETTINGS, TIMER }

enum class SummaryRange { TODAY, SEVEN_DAYS }

enum class RecordKind {
    NURSING,
    BOTTLE,
    PUMPING,
    DIAPER,
    SLEEP,
    TEMPERATURE,
    MEDICINE,
    BATH,
    MEMO,
    GROWTH,
}

sealed interface ManualRecordDraft {
    data class Bottle(val amountMl: Int, val kindCode: String) : ManualRecordDraft
    data class Pumping(
        val sideCode: String,
        val amountMl: Int?,
        val durationMinutes: Int?,
    ) : ManualRecordDraft
    data class Diaper(val pee: Boolean, val poop: Boolean, val amountCode: String?) : ManualRecordDraft
    data class Sleep(val durationMinutes: Int) : ManualRecordDraft
    data class Temperature(val celsius: Double) : ManualRecordDraft
    data class Medicine(val name: String?, val note: String?) : ManualRecordDraft
    data object Bath : ManualRecordDraft
    data class Memo(val text: String) : ManualRecordDraft
    data class Growth(val weightG: Int?, val heightMm: Int?) : ManualRecordDraft
}

enum class VoiceUiPhase {
    WAITING,
    LISTENING,
    RECOGNIZED,
    RESPONDING,
    FAILURE,
}

@Immutable
data class VoiceUiStatus(
    val phase: VoiceUiPhase = VoiceUiPhase.WAITING,
    val transcript: String? = null,
)

/**
 * A tiny state bridge that platform speech engines can update without owning UI logic.
 * String codes keep the Swift and Android service boundary stable.
 */
class VoiceUiStateStore {
    var status: VoiceUiStatus by mutableStateOf(VoiceUiStatus())
        private set

    fun update(stateCode: String, lastTranscript: String? = null) {
        val phase = when (stateCode.lowercase()) {
            "listening" -> VoiceUiPhase.LISTENING
            "recognized" -> VoiceUiPhase.RECOGNIZED
            "responding", "speaking" -> VoiceUiPhase.RESPONDING
            "failure", "error" -> VoiceUiPhase.FAILURE
            else -> VoiceUiPhase.WAITING
        }
        status = VoiceUiStatus(phase, lastTranscript?.trim()?.takeIf(String::isNotEmpty))
    }
}

@Immutable
data class NenelogUiState(
    val nowEpochMillis: Long,
    val nursingStatus: NursingCommandResult,
    val events: List<Event>,
    val todaySummary: CareSummary,
    val weekSummary: CareSummary,
    val destination: AppDestination,
    val summaryRange: SummaryRange,
    val themePreference: ThemePreference,
    val voiceStatus: VoiceUiStatus,
    val voiceControlAvailable: Boolean,
    val reducedMotion: Boolean,
    val hasDataError: Boolean = false,
)
