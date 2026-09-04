package app.nenelog.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.nenelog.data.CareSummary
import app.nenelog.data.NursingCommandResult
import app.nenelog.domain.BottleKind
import app.nenelog.domain.DiaperAmount
import app.nenelog.domain.Event
import app.nenelog.domain.EventPayload
import app.nenelog.domain.EventSource
import app.nenelog.domain.NursingSegment
import app.nenelog.domain.Side
import app.nenelog.domain.Timestamp
import app.nenelog.ui.NenelogAppContent
import app.nenelog.ui.model.AppDestination
import app.nenelog.ui.model.NenelogUiState
import app.nenelog.ui.model.SummaryRange
import app.nenelog.ui.model.VoiceUiPhase
import app.nenelog.ui.model.VoiceUiStatus
import app.nenelog.ui.theme.NenelogTheme
import app.nenelog.ui.theme.ThemePreference
import app.nenelog.ui.theme.ThemeVariant
import com.android.tools.screenshot.PreviewTest

private const val NOW = 1_788_514_200_000L
private const val MINUTE = 60_000L

@PreviewTest
@Preview(name = "Home Light", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun homeLight() = PreviewScreen(AppDestination.HOME, ThemeVariant.LIGHT)

@PreviewTest
@Preview(name = "Home Dark", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun homeDark() = PreviewScreen(AppDestination.HOME, ThemeVariant.DARK)

@PreviewTest
@Preview(name = "Home Night", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun homeNight() = PreviewScreen(AppDestination.HOME, ThemeVariant.NIGHT)

@PreviewTest
@Preview(name = "Timer Light", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun timerLight() = PreviewScreen(AppDestination.TIMER, ThemeVariant.LIGHT)

@PreviewTest
@Preview(name = "Timer Dark", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun timerDark() = PreviewScreen(AppDestination.TIMER, ThemeVariant.DARK)

@PreviewTest
@Preview(name = "Timer Night", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun timerNight() = PreviewScreen(AppDestination.TIMER, ThemeVariant.NIGHT)

@PreviewTest
@Preview(name = "Journal Light", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun journalLight() = PreviewScreen(AppDestination.JOURNAL, ThemeVariant.LIGHT)

@PreviewTest
@Preview(name = "Journal Dark", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun journalDark() = PreviewScreen(AppDestination.JOURNAL, ThemeVariant.DARK)

@PreviewTest
@Preview(name = "Journal Night", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun journalNight() = PreviewScreen(AppDestination.JOURNAL, ThemeVariant.NIGHT)

@PreviewTest
@Preview(name = "Summary Light", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun summaryLight() = PreviewScreen(AppDestination.SUMMARY, ThemeVariant.LIGHT)

@PreviewTest
@Preview(name = "Summary Dark", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun summaryDark() = PreviewScreen(AppDestination.SUMMARY, ThemeVariant.DARK)

@PreviewTest
@Preview(name = "Summary Night", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun summaryNight() = PreviewScreen(AppDestination.SUMMARY, ThemeVariant.NIGHT)

@PreviewTest
@Preview(name = "Settings Light", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun settingsLight() = PreviewScreen(AppDestination.SETTINGS, ThemeVariant.LIGHT)

@PreviewTest
@Preview(name = "Settings Dark", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun settingsDark() = PreviewScreen(AppDestination.SETTINGS, ThemeVariant.DARK)

@PreviewTest
@Preview(name = "Settings Night", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun settingsNight() = PreviewScreen(AppDestination.SETTINGS, ThemeVariant.NIGHT)

@PreviewTest
@Preview(name = "Journal Empty Japanese", widthDp = 390, heightDp = 844, locale = "ja")
@Composable
fun journalEmptyJapanese() = PreviewScreen(
    destination = AppDestination.JOURNAL,
    variant = ThemeVariant.LIGHT,
    empty = true,
)

@PreviewTest
@Preview(name = "Home Error", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun homeError() = PreviewScreen(
    destination = AppDestination.HOME,
    variant = ThemeVariant.DARK,
    error = true,
)

@PreviewTest
@Preview(
    name = "Timer Large Text Japanese",
    widthDp = 390,
    heightDp = 844,
    locale = "ja",
    fontScale = 2f,
)
@Composable
fun timerLargeTextJapanese() = PreviewScreen(AppDestination.TIMER, ThemeVariant.NIGHT)

@PreviewTest
@Preview(
    name = "Home Large Text Japanese",
    widthDp = 390,
    heightDp = 844,
    locale = "ja",
    fontScale = 2f,
)
@Composable
fun homeLargeTextJapanese() = PreviewScreen(AppDestination.HOME, ThemeVariant.LIGHT)

@PreviewTest
@Preview(
    name = "Journal Large Text English",
    widthDp = 390,
    heightDp = 844,
    locale = "en",
    fontScale = 2f,
)
@Composable
fun journalLargeTextEnglish() = PreviewScreen(AppDestination.JOURNAL, ThemeVariant.DARK)

@PreviewTest
@Preview(
    name = "Summary Large Text Japanese",
    widthDp = 390,
    heightDp = 844,
    locale = "ja",
    fontScale = 2f,
)
@Composable
fun summaryLargeTextJapanese() = PreviewScreen(AppDestination.SUMMARY, ThemeVariant.NIGHT)

@PreviewTest
@Preview(
    name = "Settings Large Text English",
    widthDp = 390,
    heightDp = 844,
    locale = "en",
    fontScale = 2f,
)
@Composable
fun settingsLargeTextEnglish() = PreviewScreen(AppDestination.SETTINGS, ThemeVariant.LIGHT)

@PreviewTest
@Preview(name = "Voice Waiting", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun voiceWaiting() = PreviewScreen(
    destination = AppDestination.HOME,
    variant = ThemeVariant.LIGHT,
    voiceStatusOverride = VoiceUiStatus(VoiceUiPhase.WAITING),
)

@PreviewTest
@Preview(name = "Voice Recognized", widthDp = 390, heightDp = 844, locale = "ja")
@Composable
fun voiceRecognized() = PreviewScreen(
    destination = AppDestination.HOME,
    variant = ThemeVariant.DARK,
    voiceStatusOverride = VoiceUiStatus(VoiceUiPhase.RECOGNIZED, "右スタート"),
)

@PreviewTest
@Preview(name = "Voice Failure", widthDp = 390, heightDp = 844, locale = "en")
@Composable
fun voiceFailure() = PreviewScreen(
    destination = AppDestination.HOME,
    variant = ThemeVariant.NIGHT,
    voiceStatusOverride = VoiceUiStatus(VoiceUiPhase.FAILURE),
)

@Composable
private fun PreviewScreen(
    destination: AppDestination,
    variant: ThemeVariant,
    empty: Boolean = false,
    error: Boolean = false,
    voiceStatusOverride: VoiceUiStatus? = null,
) {
    val events = if (empty) emptyList() else sampleEvents()
    val summary = if (empty) emptySummary() else CareSummary(
        nursingCount = 3,
        nursingDurationMillis = 42 * MINUTE,
        bottleCount = 2,
        bottleAmountMl = 170,
        diaperCount = 4,
        poopCount = 2,
        sleepDurationMillis = 186 * MINUTE,
        memoCount = 1,
    )
    val running = destination == AppDestination.TIMER
    val state = NenelogUiState(
        nowEpochMillis = NOW,
        nursingStatus = NursingCommandResult(
            success = true,
            responseJa = "",
            responseEn = "",
            stateCode = if (running) "running" else "idle",
            currentSideCode = if (running) "right" else null,
            elapsedMillis = if (running) 8 * MINUTE + 34_000 else 0,
        ),
        events = events,
        todaySummary = summary,
        weekSummary = summary.copy(
            nursingCount = 18,
            nursingDurationMillis = 241 * MINUTE,
            bottleCount = 11,
            bottleAmountMl = 940,
            diaperCount = 29,
            poopCount = 12,
            sleepDurationMillis = 1_502 * MINUTE,
        ),
        destination = destination,
        summaryRange = SummaryRange.TODAY,
        themePreference = when (variant) {
            ThemeVariant.LIGHT -> ThemePreference.LIGHT
            ThemeVariant.DARK -> ThemePreference.DARK
            ThemeVariant.NIGHT -> ThemePreference.NIGHT
        },
        voiceStatus = voiceStatusOverride ?: VoiceUiStatus(
            phase = if (running) VoiceUiPhase.RESPONDING else VoiceUiPhase.LISTENING,
            transcript = if (running) "Right" else null,
        ),
        voiceControlAvailable = true,
        reducedMotion = true,
        hasDataError = error,
    )
    NenelogTheme(variant) {
        NenelogAppContent(state = state)
    }
}

private fun sampleEvents(): List<Event> {
    val nursingStart = Timestamp(NOW - 190 * MINUTE)
    return listOf(
        event(
            id = "memo",
            occurredAt = NOW - 18 * MINUTE,
            payload = EventPayload.Memo("Smiled back during the morning song."),
        ),
        event(
            id = "diaper",
            occurredAt = NOW - 47 * MINUTE,
            payload = EventPayload.Diaper(pee = true, poop = true, amount = DiaperAmount.M),
        ),
        event(
            id = "bottle",
            occurredAt = NOW - 112 * MINUTE,
            payload = EventPayload.Bottle(90, BottleKind.BREAST_MILK),
        ),
        event(
            id = "nursing",
            occurredAt = nursingStart.epochMillis,
            source = EventSource.VOICE_L2,
            payload = EventPayload.Nursing(
                segments = listOf(
                    NursingSegment(Side.LEFT, nursingStart, nursingStart.plusMillis(8 * MINUTE + 12_000)),
                    NursingSegment(
                        Side.RIGHT,
                        nursingStart.plusMillis(8 * MINUTE + 12_000),
                        nursingStart.plusMillis(14 * MINUTE + 27_000),
                    ),
                ),
            ),
        ),
        event(
            id = "sleep",
            occurredAt = NOW - 330 * MINUTE,
            payload = EventPayload.Sleep(
                startedAt = Timestamp(NOW - 330 * MINUTE),
                endedAt = Timestamp(NOW - 186 * MINUTE),
            ),
        ),
    )
}

private fun event(
    id: String,
    occurredAt: Long,
    payload: EventPayload,
    source: EventSource = EventSource.TAP,
): Event = Event(
    id = id,
    babyId = "preview-baby",
    caregiverId = "preview-caregiver",
    occurredAt = Timestamp(occurredAt),
    createdAt = Timestamp(occurredAt),
    source = source,
    payload = payload,
)

private fun emptySummary() = CareSummary(
    nursingCount = 0,
    nursingDurationMillis = 0,
    bottleCount = 0,
    bottleAmountMl = 0,
    diaperCount = 0,
    poopCount = 0,
    sleepDurationMillis = 0,
    memoCount = 0,
)
