package app.nenelog.data

import app.nenelog.domain.BottleKind
import app.nenelog.domain.DiaperAmount
import app.nenelog.domain.Event
import app.nenelog.domain.EventId
import app.nenelog.domain.EventPayload
import app.nenelog.domain.EventSource
import app.nenelog.domain.EventStore
import app.nenelog.domain.Side
import app.nenelog.domain.Timestamp

/**
 * All non-session care records use the same append-only store as nursing.
 * UI, voice, Siri, and future sync code therefore observe one durable timeline.
 */
class CareLogService(
    private val store: EventStore,
    private val babyId: String = DEFAULT_BABY_ID,
    private val caregiverId: String = DEFAULT_CAREGIVER_ID,
) {
    fun timeline(): List<Event> = store.active().sortedByDescending { it.occurredAt }

    fun recordBottle(
        amountMl: Int,
        kindCode: String,
        epochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        require(amountMl in 1..1_000) { "bottle amount must be between 1 and 1000 ml" }
        val kind = BottleKind.entries.firstOrNull { it.name.equals(kindCode, ignoreCase = true) }
            ?: error("unsupported bottle kind: $kindCode")
        return record(EventPayload.Bottle(amountMl, kind), epochMillis, sourceCode)
    }

    fun recordPumping(
        sideCode: String,
        amountMl: Int?,
        durationMinutes: Int?,
        epochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        require(amountMl == null || amountMl in 1..1_000) { "pumping amount is out of range" }
        require(durationMinutes == null || durationMinutes in 1..720) {
            "pumping duration is out of range"
        }
        require(amountMl != null || durationMinutes != null) {
            "pumping needs an amount or a duration"
        }
        return record(
            EventPayload.Pumping(
                side = sideCode.toSide(),
                amountMl = amountMl,
                durationSec = durationMinutes?.times(60),
            ),
            epochMillis,
            sourceCode,
        )
    }

    fun recordDiaper(
        pee: Boolean,
        poop: Boolean,
        amountCode: String?,
        epochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        require(pee || poop) { "a diaper record needs pee, poop, or both" }
        val amount = amountCode?.let { code ->
            DiaperAmount.entries.firstOrNull { it.name.equals(code, ignoreCase = true) }
                ?: error("unsupported diaper amount: $code")
        }
        return record(EventPayload.Diaper(pee, poop, amount), epochMillis, sourceCode)
    }

    fun recordSleep(
        durationMinutes: Int,
        endedAtEpochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        require(durationMinutes in 1..1_440) { "sleep duration must be between 1 and 1440 minutes" }
        val endedAt = Timestamp(endedAtEpochMillis)
        return record(
            payload = EventPayload.Sleep(
                startedAt = endedAt.plusMillis(-durationMinutes * 60_000L),
                endedAt = endedAt,
            ),
            epochMillis = endedAtEpochMillis,
            sourceCode = sourceCode,
            occurredAt = endedAt.plusMillis(-durationMinutes * 60_000L),
        )
    }

    fun recordTemperature(
        celsius: Double,
        epochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        require(celsius in 30.0..45.0) { "temperature is out of range" }
        return record(EventPayload.Temperature(celsius), epochMillis, sourceCode)
    }

    fun recordMedicine(
        name: String?,
        note: String?,
        epochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        val cleanName = name?.trim()?.takeIf(String::isNotEmpty)
        val cleanNote = note?.trim()?.takeIf(String::isNotEmpty)
        require(cleanName != null || cleanNote != null) { "medicine needs a name or note" }
        return record(EventPayload.Medicine(cleanName, cleanNote), epochMillis, sourceCode)
    }

    fun recordBath(epochMillis: Long, sourceCode: String = SOURCE_TAP): Event =
        record(EventPayload.Bath, epochMillis, sourceCode)

    fun recordMemo(
        text: String,
        epochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty()) { "memo must not be blank" }
        require(cleanText.length <= MAX_MEMO_LENGTH) { "memo is too long" }
        return record(EventPayload.Memo(cleanText), epochMillis, sourceCode)
    }

    fun recordGrowth(
        weightG: Int?,
        heightMm: Int?,
        epochMillis: Long,
        sourceCode: String = SOURCE_TAP,
    ): Event {
        require(weightG == null || weightG in 100..100_000) { "weight is out of range" }
        require(heightMm == null || heightMm in 100..2_500) { "height is out of range" }
        require(weightG != null || heightMm != null) { "growth needs weight or height" }
        return record(EventPayload.Growth(weightG, heightMm), epochMillis, sourceCode)
    }

    fun summary(
        fromEpochMillis: Long,
        untilEpochMillis: Long,
    ): CareSummary {
        require(untilEpochMillis >= fromEpochMillis) { "invalid summary range" }
        var nursingCount = 0
        var nursingDurationMillis = 0L
        var bottleCount = 0
        var bottleAmountMl = 0
        var diaperCount = 0
        var poopCount = 0
        var sleepDurationMillis = 0L
        var memoCount = 0

        store.active().forEach { event ->
            when (val payload = event.payload) {
                is EventPayload.Sleep -> {
                    val endedAt = payload.endedAt?.epochMillis ?: untilEpochMillis
                    if (payload.startedAt.epochMillis < untilEpochMillis && endedAt > fromEpochMillis) {
                        val clippedStart = maxOf(payload.startedAt.epochMillis, fromEpochMillis)
                        val clippedEnd = minOf(endedAt, untilEpochMillis)
                        sleepDurationMillis += (clippedEnd - clippedStart).coerceAtLeast(0)
                    }
                }
                else -> if (event.occurredAt.epochMillis in fromEpochMillis until untilEpochMillis) {
                    when (payload) {
                        is EventPayload.Nursing -> {
                            nursingCount++
                            nursingDurationMillis += payload.totalDurationMillis
                        }
                        is EventPayload.Bottle -> {
                            bottleCount++
                            bottleAmountMl += payload.amountMl
                        }
                        is EventPayload.Diaper -> {
                            diaperCount++
                            if (payload.poop) poopCount++
                        }
                        is EventPayload.Memo -> memoCount++
                        else -> Unit
                    }
                }
            }
        }

        return CareSummary(
            nursingCount = nursingCount,
            nursingDurationMillis = nursingDurationMillis,
            bottleCount = bottleCount,
            bottleAmountMl = bottleAmountMl,
            diaperCount = diaperCount,
            poopCount = poopCount,
            sleepDurationMillis = sleepDurationMillis,
            memoCount = memoCount,
        )
    }

    fun revoke(eventId: String, epochMillis: Long): Boolean =
        store.revoke(eventId, caregiverId, Timestamp(epochMillis)) != null

    fun undoLast(epochMillis: Long): Event? =
        store.undoLast(caregiverId, Timestamp(epochMillis))

    private fun record(
        payload: EventPayload,
        epochMillis: Long,
        sourceCode: String,
        occurredAt: Timestamp = Timestamp(epochMillis),
    ): Event {
        val createdAt = Timestamp(epochMillis)
        val event = Event(
            id = EventId.new(createdAt),
            babyId = babyId,
            caregiverId = caregiverId,
            occurredAt = occurredAt,
            createdAt = createdAt,
            source = sourceCode.toEventSource(),
            payload = payload,
        )
        store.append(event)
        return event
    }

    private fun String.toSide(): Side = when (trim().lowercase()) {
        "left" -> Side.LEFT
        "right" -> Side.RIGHT
        else -> error("unsupported side: $this")
    }

    private fun String.toEventSource(): EventSource =
        EventSource.entries.firstOrNull { it.name.equals(trim(), ignoreCase = true) }
            ?: error("unsupported event source: $this")

    companion object {
        const val SOURCE_TAP = "tap"
        private const val MAX_MEMO_LENGTH = 2_000
        private const val DEFAULT_BABY_ID = "local-baby"
        private const val DEFAULT_CAREGIVER_ID = "local-caregiver"
    }
}

data class CareSummary(
    val nursingCount: Int,
    val nursingDurationMillis: Long,
    val bottleCount: Int,
    val bottleAmountMl: Int,
    val diaperCount: Int,
    val poopCount: Int,
    val sleepDurationMillis: Long,
    val memoCount: Int,
) {
    val feedingCount: Int get() = nursingCount + bottleCount
    val isEmpty: Boolean
        get() = nursingCount == 0 && bottleCount == 0 && diaperCount == 0 &&
            sleepDurationMillis == 0L && memoCount == 0
}
