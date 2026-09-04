package app.nenelog.data

import app.nenelog.domain.Event
import app.nenelog.domain.EventId
import app.nenelog.domain.EventPayload
import app.nenelog.domain.EventSource
import app.nenelog.domain.NursingResponseComposer
import app.nenelog.domain.NursingSessionStateMachine
import app.nenelog.domain.Side
import app.nenelog.domain.Timestamp

/**
 * 音声・Siri・画面タップの全経路が使う、授乳セッションの単一窓口。
 *
 * ActiveSession は操作ごとに永続化し、停止時だけ不変 Event に確定する。
 * 時刻は呼び出し側から注入するため、実時間に依存せずテストできる。
 */
class NursingSessionService(
    private val store: NursingSessionStore,
    private val babyId: String = DEFAULT_BABY_ID,
    private val caregiverId: String = DEFAULT_CAREGIVER_ID,
    private val timezoneId: String = DEFAULT_TIMEZONE_ID,
) {

    fun start(
        sideCode: String,
        epochMillis: Long,
        sourceCode: String,
    ): NursingCommandResult {
        val at = Timestamp(epochMillis)
        val side = sideCode.toSide()
        val requestedSource = sourceCode.toEventSource()
        var stored = store.loadActiveSession()
        if (stored != null && completeIfExpired(stored, at) != null) {
            stored = null
        }
        val machine = stored?.snapshot
            ?.let(NursingSessionStateMachine::restore)
            ?: NursingSessionStateMachine().also { it.start(babyId, side, at) }

        if (stored != null) {
            when (machine.state) {
                is NursingSessionStateMachine.State.Running -> machine.switchSide(side, at)
                is NursingSessionStateMachine.State.Paused -> machine.resume(at, side)
                else -> error("active session snapshot restored to a terminal state")
            }
        }

        val session = StoredNursingSession(
            snapshot = requireNotNull(machine.snapshot()),
            source = stored?.source ?: requestedSource,
            caregiverId = stored?.caregiverId ?: caregiverId,
            timezoneId = stored?.timezoneId ?: timezoneId,
        )
        store.saveActiveSession(session, at)

        val response = NursingResponseComposer.started(side)
        return NursingCommandResult(
            success = true,
            responseJa = response.ja,
            responseEn = response.en,
            stateCode = STATE_RUNNING,
            currentSideCode = side.toCode(),
            elapsedMillis = machine.elapsedMillis(at),
        )
    }

    fun stop(epochMillis: Long): NursingCommandResult {
        val at = Timestamp(epochMillis)
        val stored = store.loadActiveSession() ?: NursingResponseComposer.noTimer().let { response ->
            return idleResult(responseJa = response.ja, responseEn = response.en)
        }

        completeIfExpired(stored, at)?.let { completed ->
            return completedResult(completed)
        }

        val machine = NursingSessionStateMachine.restore(stored.snapshot)
        val payload = machine.stop(at)
        return completedResult(complete(stored, payload, at))
    }

    fun status(epochMillis: Long): NursingCommandResult {
        val at = Timestamp(epochMillis)
        val stored = store.loadActiveSession()
            ?: return idleResult(responseJa = "停止中", responseEn = "Stopped")
        completeIfExpired(stored, at)?.let { completed ->
            return completedResult(completed)
        }
        val machine = NursingSessionStateMachine.restore(stored.snapshot)
        val state = machine.state
        val side = when (state) {
            is NursingSessionStateMachine.State.Running -> state.side
            is NursingSessionStateMachine.State.Paused -> state.lastSide
            else -> null
        }
        return NursingCommandResult(
            success = true,
            responseJa = when (state) {
                is NursingSessionStateMachine.State.Running ->
                    "${side?.labelJa()}、${machine.elapsedMillis(at).roundedMinutes()}分です"
                is NursingSessionStateMachine.State.Paused -> "一時停止中"
                else -> "停止中"
            },
            responseEn = when (state) {
                is NursingSessionStateMachine.State.Running ->
                    "${side?.labelEn()}, ${machine.elapsedMillis(at).roundedMinutes()} minutes."
                is NursingSessionStateMachine.State.Paused -> "Paused"
                else -> "Stopped"
            },
            stateCode = if (state is NursingSessionStateMachine.State.Paused) STATE_PAUSED else STATE_RUNNING,
            currentSideCode = side?.toCode(),
            elapsedMillis = machine.elapsedMillis(at),
        )
    }

    fun timeline(): List<NursingTimelineItem> = store.active()
        .mapNotNull { event ->
            val payload = event.payload as? EventPayload.Nursing ?: return@mapNotNull null
            NursingTimelineItem(
                id = event.id,
                occurredAtEpochMillis = event.occurredAt.epochMillis,
                endedAtEpochMillis = payload.segments.maxOfOrNull { it.endedAt.epochMillis }
                    ?: event.createdAt.epochMillis,
                totalDurationMillis = payload.totalDurationMillis,
                leftDurationMillis = payload.segments
                    .filter { it.side == Side.LEFT }
                    .sumOf { it.durationMillis },
                rightDurationMillis = payload.segments
                    .filter { it.side == Side.RIGHT }
                    .sumOf { it.durationMillis },
                sourceCode = event.source.name.lowercase(),
                autoClosed = payload.autoClosed,
            )
        }
        .sortedByDescending { it.occurredAtEpochMillis }

    private fun completeIfExpired(
        stored: StoredNursingSession,
        at: Timestamp,
    ): CompletedSession? {
        val machine = NursingSessionStateMachine.restore(stored.snapshot)
        val payload = machine.tick(at) ?: return null
        return complete(stored, payload, at)
    }

    private fun complete(
        stored: StoredNursingSession,
        payload: EventPayload.Nursing,
        at: Timestamp,
    ): CompletedSession {
        val event = Event(
            id = EventId.new(at),
            babyId = stored.snapshot.babyId,
            caregiverId = stored.caregiverId,
            occurredAt = stored.snapshot.startedAt,
            createdAt = at,
            source = stored.source,
            payload = payload,
        )
        store.completeActiveSession(event)
        return CompletedSession(event, payload)
    }

    private fun completedResult(completed: CompletedSession): NursingCommandResult {
        val response = NursingResponseComposer.stopped(completed.payload)
        return NursingCommandResult(
            success = true,
            responseJa = response.ja,
            responseEn = response.en,
            stateCode = STATE_IDLE,
            elapsedMillis = completed.payload.totalDurationMillis,
            recordedEventId = completed.event.id,
        )
    }

    private fun idleResult(responseJa: String, responseEn: String) = NursingCommandResult(
        success = false,
        responseJa = responseJa,
        responseEn = responseEn,
        stateCode = STATE_IDLE,
        elapsedMillis = 0,
    )

    private data class CompletedSession(
        val event: Event,
        val payload: EventPayload.Nursing,
    )

    companion object {
        const val SOURCE_TAP = "tap"
        const val SOURCE_VOICE_L2 = "voice_l2"
        const val SOURCE_SIRI = "siri"

        private const val DEFAULT_BABY_ID = "local-baby"
        private const val DEFAULT_CAREGIVER_ID = "local-caregiver"
        private const val DEFAULT_TIMEZONE_ID = "device-local"
        private const val STATE_IDLE = "idle"
        private const val STATE_RUNNING = "running"
        private const val STATE_PAUSED = "paused"
    }
}

data class NursingCommandResult(
    val success: Boolean,
    val responseJa: String,
    val responseEn: String,
    val stateCode: String,
    val currentSideCode: String? = null,
    val elapsedMillis: Long,
    val recordedEventId: String? = null,
)

data class NursingTimelineItem(
    val id: String,
    val occurredAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val totalDurationMillis: Long,
    val leftDurationMillis: Long,
    val rightDurationMillis: Long,
    val sourceCode: String,
    val autoClosed: Boolean,
)

fun createNursingSessionService(
    factory: DatabaseDriverFactory,
    timezoneId: String = "device-local",
): NursingSessionService = createNenelogServices(factory, timezoneId).nursing

private fun String.toSide(): Side = when (trim().lowercase()) {
    "left" -> Side.LEFT
    "right" -> Side.RIGHT
    else -> error("unsupported nursing side: $this")
}

private fun String.toEventSource(): EventSource =
    EventSource.entries.firstOrNull { it.name.equals(trim(), ignoreCase = true) }
        ?: error("unsupported event source: $this")

private fun Side.toCode(): String = name.lowercase()
private fun Side.labelJa(): String = if (this == Side.RIGHT) "右" else "左"
private fun Side.labelEn(): String = if (this == Side.RIGHT) "Right" else "Left"

private fun Long.roundedMinutes(): Long = when {
    this <= 0 -> 0
    this < 60_000 -> 1
    else -> (this + 30_000) / 60_000
}
