package app.nenelog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.nenelog.data.CareSummary
import app.nenelog.data.NenelogServices
import app.nenelog.data.NursingCommandResult
import app.nenelog.domain.Event
import app.nenelog.resources.*
import app.nenelog.ui.components.DataErrorState
import app.nenelog.ui.components.LinenBottomNavigation
import app.nenelog.ui.components.QuickRecordBar
import app.nenelog.ui.model.AppDestination
import app.nenelog.ui.model.ManualRecordDraft
import app.nenelog.ui.model.NenelogUiState
import app.nenelog.ui.model.RecordKind
import app.nenelog.ui.model.SummaryRange
import app.nenelog.ui.model.VoiceUiStateStore
import app.nenelog.ui.platform.localHour
import app.nenelog.ui.platform.platformReducedMotionEnabled
import app.nenelog.ui.platform.startOfLocalDay
import app.nenelog.ui.platform.startOfRollingWeek
import app.nenelog.ui.screens.EventDetailDialog
import app.nenelog.ui.screens.HomeScreen
import app.nenelog.ui.screens.JournalScreen
import app.nenelog.ui.screens.NursingTimerScreen
import app.nenelog.ui.screens.RecordSheet
import app.nenelog.ui.screens.SettingsScreen
import app.nenelog.ui.screens.SummaryScreen
import app.nenelog.ui.theme.NenelogTheme
import app.nenelog.ui.theme.ThemePreference
import app.nenelog.ui.theme.resolveThemeVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun App(
    services: NenelogServices,
    voiceControlAvailable: Boolean = false,
    voiceStateStore: VoiceUiStateStore? = null,
    onVoiceSessionStart: () -> Unit = {},
    onVoiceSessionStop: () -> Unit = {},
) {
    val resolvedVoiceStateStore = voiceStateStore ?: remember { VoiceUiStateStore() }
    val initialNow = remember { nowEpochMillis() }
    val initialSnapshot = remember(services) { loadSnapshot(services, initialNow) }
    var now by remember { mutableStateOf(initialNow) }
    var nursingStatus by remember { mutableStateOf(initialSnapshot.nursingStatus) }
    var events by remember { mutableStateOf(initialSnapshot.events) }
    var todaySummary by remember { mutableStateOf(initialSnapshot.todaySummary) }
    var weekSummary by remember { mutableStateOf(initialSnapshot.weekSummary) }
    var themePreference by remember { mutableStateOf(initialSnapshot.themePreference) }
    var hasDataError by remember { mutableStateOf(initialSnapshot.hasError) }
    var destination by remember { mutableStateOf(AppDestination.HOME) }
    var summaryRange by remember { mutableStateOf(SummaryRange.TODAY) }
    var recordSheetKind by remember { mutableStateOf<RecordKind?>(null) }
    var showRecordSheet by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(Res.string.saved)
    val undoLabel = stringResource(Res.string.undo)
    val reducedMotion = platformReducedMotionEnabled()

    fun refreshAll(at: Long = nowEpochMillis()): Boolean {
        val updated = loadSnapshot(services, at)
        now = at
        if (updated.hasError) {
            hasDataError = true
            return false
        }
        nursingStatus = updated.nursingStatus
        events = updated.events
        todaySummary = updated.todaySummary
        weekSummary = updated.weekSummary
        themePreference = updated.themePreference
        hasDataError = false
        return true
    }

    fun showSavedWithUndo(eventId: String) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = savedMessage,
                actionLabel = undoLabel,
                withDismissAction = true,
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                runCatching { services.careLog.revoke(eventId, nowEpochMillis()) }
                    .onSuccess { refreshAll() }
                    .onFailure { hasDataError = true }
            }
        }
    }

    fun saveManualRecord(draft: ManualRecordDraft): Boolean {
        val at = nowEpochMillis()
        return runCatching {
            when (draft) {
                is ManualRecordDraft.Bottle -> services.careLog.recordBottle(
                    draft.amountMl,
                    draft.kindCode,
                    at,
                )
                is ManualRecordDraft.Pumping -> services.careLog.recordPumping(
                    draft.sideCode,
                    draft.amountMl,
                    draft.durationMinutes,
                    at,
                )
                is ManualRecordDraft.Diaper -> services.careLog.recordDiaper(
                    draft.pee,
                    draft.poop,
                    draft.amountCode,
                    at,
                )
                is ManualRecordDraft.Sleep -> services.careLog.recordSleep(draft.durationMinutes, at)
                is ManualRecordDraft.Temperature -> services.careLog.recordTemperature(draft.celsius, at)
                is ManualRecordDraft.Medicine -> services.careLog.recordMedicine(draft.name, draft.note, at)
                ManualRecordDraft.Bath -> services.careLog.recordBath(at)
                is ManualRecordDraft.Memo -> services.careLog.recordMemo(draft.text, at)
                is ManualRecordDraft.Growth -> services.careLog.recordGrowth(
                    draft.weightG,
                    draft.heightMm,
                    at,
                )
            }
        }.fold(
            onSuccess = { savedEvent ->
                showRecordSheet = false
                recordSheetKind = null
                refreshAll(at)
                showSavedWithUndo(savedEvent.id)
                true
            },
            onFailure = { error ->
                if (error !is IllegalArgumentException) hasDataError = true
                false
            },
        )
    }

    LifecycleResumeEffect(services, voiceControlAvailable) {
        refreshAll()
        if (nursingStatus.stateCode == "running") {
            resolvedVoiceStateStore.update("waiting")
            onVoiceSessionStart()
        }
        onPauseOrDispose { }
    }

    LaunchedEffect(nursingStatus.stateCode) {
        while (true) {
            delay(if (nursingStatus.stateCode == "running") 1_000 else 30_000)
            val tickNow = nowEpochMillis()
            now = tickNow
            if (nursingStatus.stateCode == "running" || nursingStatus.stateCode == "paused") {
                runCatching { services.nursing.status(tickNow) }
                    .onSuccess { updatedStatus ->
                        val completed = updatedStatus.stateCode == "idle"
                        nursingStatus = updatedStatus
                        if (completed) {
                            onVoiceSessionStop()
                            resolvedVoiceStateStore.update("waiting")
                            refreshAll(tickNow)
                        }
                    }
                    .onFailure { hasDataError = true }
            }
        }
    }

    val themeVariant = resolveThemeVariant(
        preference = themePreference,
        systemDark = isSystemInDarkTheme(),
        localHour = localHour(now),
    )
    val uiState = NenelogUiState(
        nowEpochMillis = now,
        nursingStatus = nursingStatus,
        events = events,
        todaySummary = todaySummary,
        weekSummary = weekSummary,
        destination = destination,
        summaryRange = summaryRange,
        themePreference = themePreference,
        voiceStatus = resolvedVoiceStateStore.status,
        voiceControlAvailable = voiceControlAvailable,
        reducedMotion = reducedMotion,
        hasDataError = hasDataError,
    )

    NenelogTheme(themeVariant) {
        NenelogAppContent(
            state = uiState,
            snackbarHostState = snackbarHostState,
            selectedEvent = selectedEvent,
            onRetry = { refreshAll() },
            onDestinationSelected = { destination = it },
            onOpenTimer = { destination = AppDestination.TIMER },
            onStartSide = { side ->
                val wasRunning = nursingStatus.stateCode == "running"
                runCatching {
                    services.nursing.start(
                        sideCode = side,
                        epochMillis = nowEpochMillis(),
                        sourceCode = "tap",
                    )
                }.onSuccess {
                    nursingStatus = it
                    if (!wasRunning) {
                        resolvedVoiceStateStore.update("waiting")
                        onVoiceSessionStart()
                    }
                    refreshAll()
                }.onFailure { hasDataError = true }
            },
            onStop = {
                runCatching { services.nursing.stop(nowEpochMillis()) }
                    .onSuccess { stopped ->
                        onVoiceSessionStop()
                        resolvedVoiceStateStore.update("waiting")
                        refreshAll()
                        destination = AppDestination.HOME
                        stopped.recordedEventId?.let(::showSavedWithUndo)
                    }
                    .onFailure { hasDataError = true }
            },
            onSummaryRangeSelected = { summaryRange = it },
            onThemeSelected = { selected ->
                runCatching { services.settings.setThemePreferenceCode(selected.storageCode) }
                    .onSuccess { themePreference = selected }
                    .onFailure { hasDataError = true }
            },
            onRecordSelected = { kind ->
                if (kind == RecordKind.NURSING) {
                    destination = AppDestination.TIMER
                } else {
                    recordSheetKind = kind
                    showRecordSheet = true
                }
            },
            onEventSelected = { selectedEvent = it },
            onEventDismissed = { selectedEvent = null },
            onEventDeleted = {
                val target = selectedEvent
                if (target != null) {
                    runCatching { services.careLog.revoke(target.id, nowEpochMillis()) }
                        .onSuccess {
                            selectedEvent = null
                            refreshAll()
                        }
                        .onFailure { hasDataError = true }
                }
            },
        )
        if (showRecordSheet) {
            RecordSheet(
                initialKind = recordSheetKind,
                onDismiss = {
                    showRecordSheet = false
                    recordSheetKind = null
                },
                onOpenNursingTimer = {
                    showRecordSheet = false
                    recordSheetKind = null
                    destination = AppDestination.TIMER
                },
                onSave = ::saveManualRecord,
            )
        }
    }
}

@Composable
fun NenelogAppContent(
    state: NenelogUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    selectedEvent: Event? = null,
    onRetry: () -> Unit = {},
    onDestinationSelected: (AppDestination) -> Unit = {},
    onOpenTimer: () -> Unit = {},
    onStartSide: (String) -> Unit = {},
    onStop: () -> Unit = {},
    onSummaryRangeSelected: (SummaryRange) -> Unit = {},
    onThemeSelected: (ThemePreference) -> Unit = {},
    onRecordSelected: (RecordKind?) -> Unit = {},
    onEventSelected: (Event) -> Unit = {},
    onEventDismissed: () -> Unit = {},
    onEventDeleted: () -> Unit = {},
) {
    val showNavigation = state.destination != AppDestination.TIMER
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showNavigation) {
                Column {
                    if (!state.hasDataError && (
                            state.destination == AppDestination.HOME ||
                                state.destination == AppDestination.JOURNAL
                            )
                    ) {
                        QuickRecordBar(onRecordSelected = onRecordSelected)
                    }
                    LinenBottomNavigation(
                        selectedDestination = state.destination,
                        onDestinationSelected = onDestinationSelected,
                    )
                }
            }
        },
    ) { contentPadding ->
        if (state.hasDataError) {
            DataErrorState(
                onRetry = onRetry,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            val screenModifier = Modifier.padding(contentPadding)
            when (state.destination) {
                AppDestination.HOME -> HomeScreen(
                    nowEpochMillis = state.nowEpochMillis,
                    nursingStatus = state.nursingStatus,
                    events = state.events,
                    voiceStatus = state.voiceStatus,
                    voiceControlAvailable = state.voiceControlAvailable,
                    reducedMotion = state.reducedMotion,
                    onOpenTimer = onOpenTimer,
                    onOpenJournal = { onDestinationSelected(AppDestination.JOURNAL) },
                    onEventSelected = onEventSelected,
                    modifier = screenModifier,
                )
                AppDestination.JOURNAL -> JournalScreen(
                    events = state.events,
                    onEventSelected = onEventSelected,
                    modifier = screenModifier,
                )
                AppDestination.SUMMARY -> {
                    val start = if (state.summaryRange == SummaryRange.TODAY) {
                        startOfLocalDay(state.nowEpochMillis)
                    } else {
                        startOfRollingWeek(state.nowEpochMillis)
                    }
                    SummaryScreen(
                        selectedRange = state.summaryRange,
                        summary = if (state.summaryRange == SummaryRange.TODAY) {
                            state.todaySummary
                        } else {
                            state.weekSummary
                        },
                        events = state.events,
                        rangeStartEpochMillis = start,
                        rangeEndEpochMillis = state.nowEpochMillis + 1,
                        onRangeSelected = onSummaryRangeSelected,
                        modifier = screenModifier,
                    )
                }
                AppDestination.SETTINGS -> SettingsScreen(
                    selectedTheme = state.themePreference,
                    voiceStatus = state.voiceStatus,
                    voiceControlAvailable = state.voiceControlAvailable,
                    reducedMotion = state.reducedMotion,
                    onThemeSelected = onThemeSelected,
                    modifier = screenModifier,
                )
                AppDestination.TIMER -> NursingTimerScreen(
                    status = state.nursingStatus,
                    voiceStatus = state.voiceStatus,
                    voiceControlAvailable = state.voiceControlAvailable,
                    reducedMotion = state.reducedMotion,
                    onBack = { onDestinationSelected(AppDestination.HOME) },
                    onStartSide = onStartSide,
                    onStop = onStop,
                    modifier = screenModifier,
                )
            }
        }
    }

    if (selectedEvent != null) {
        EventDetailDialog(
            event = selectedEvent,
            onDismiss = onEventDismissed,
            onDelete = onEventDeleted,
        )
    }
}

private data class LoadedSnapshot(
    val nursingStatus: NursingCommandResult,
    val events: List<Event>,
    val todaySummary: CareSummary,
    val weekSummary: CareSummary,
    val themePreference: ThemePreference,
    val hasError: Boolean,
)

private fun loadSnapshot(services: NenelogServices, now: Long): LoadedSnapshot = runCatching {
    val nursingStatus = services.nursing.status(now)
    val events = services.careLog.timeline().toList()
    LoadedSnapshot(
        nursingStatus = nursingStatus,
        events = events,
        todaySummary = services.careLog.summary(startOfLocalDay(now), now + 1),
        weekSummary = services.careLog.summary(startOfRollingWeek(now), now + 1),
        themePreference = ThemePreference.fromStorageCode(services.settings.themePreferenceCode()),
        hasError = false,
    )
}.getOrElse {
    LoadedSnapshot(
        nursingStatus = NursingCommandResult(
            success = false,
            responseJa = "",
            responseEn = "",
            stateCode = "idle",
            elapsedMillis = 0,
        ),
        events = emptyList(),
        todaySummary = emptySummary(),
        weekSummary = emptySummary(),
        themePreference = ThemePreference.AUTO,
        hasError = true,
    )
}

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

@OptIn(kotlin.time.ExperimentalTime::class)
private fun nowEpochMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
