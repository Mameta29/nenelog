package app.nenelog.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Side { LEFT, RIGHT }

/** docs/06: 音声経由率KPI・Layers実験のため source は必須 */
enum class EventSource { VOICE_L1, VOICE_L2, SIRI, TAP, WIDGET, IMPORT }

enum class BottleKind { FORMULA, BREAST_MILK, MIXED }

enum class DiaperAmount { S, M, L }

@Serializable
data class NursingSegment(
    val side: Side,
    val startedAt: Timestamp,
    val endedAt: Timestamp,
) {
    val durationMillis: Long get() = endedAt - startedAt
}

/** docs/06 の type別 payload。追記型イベントの中身 */
@Serializable
sealed interface EventPayload {

    @Serializable
    @SerialName("nursing")
    data class Nursing(
        val segments: List<NursingSegment>,
        val note: String? = null,
        val autoClosed: Boolean = false,
    ) : EventPayload {
        val totalDurationMillis: Long get() = segments.sumOf { it.durationMillis }
    }

    @Serializable
    @SerialName("bottle")
    data class Bottle(val amountMl: Int, val kind: BottleKind) : EventPayload

    @Serializable
    @SerialName("pumping")
    data class Pumping(
        val side: Side,
        val amountMl: Int? = null,
        val durationSec: Int? = null,
    ) : EventPayload

    @Serializable
    @SerialName("diaper")
    data class Diaper(
        val pee: Boolean,
        val poop: Boolean,
        val amount: DiaperAmount? = null,
        val consistency: String? = null,
    ) : EventPayload

    @Serializable
    @SerialName("sleep")
    data class Sleep(
        val startedAt: Timestamp,
        val endedAt: Timestamp? = null, // null = 睡眠中
    ) : EventPayload

    @Serializable
    @SerialName("temperature")
    data class Temperature(val celsius: Double) : EventPayload

    @Serializable
    @SerialName("medicine")
    data class Medicine(val name: String? = null, val note: String? = null) : EventPayload

    @Serializable
    @SerialName("bath")
    data object Bath : EventPayload

    @Serializable
    @SerialName("growth")
    data class Growth(val weightG: Int? = null, val heightMm: Int? = null) : EventPayload

    @Serializable
    @SerialName("memo")
    data class Memo(val text: String) : EventPayload
    // aiParsed(AI構造化の下書き)は P1 実装時に追加(docs/06)

    /** 打ち消しイベント。「取り消し」undo は対象を消さずこれを追記する(docs/06) */
    @Serializable
    @SerialName("revocation")
    data class Revocation(val targetEventId: String) : EventPayload
}

@Serializable
data class Event(
    val id: String, // UUIDv7
    val babyId: String,
    val caregiverId: String,
    val occurredAt: Timestamp,
    val createdAt: Timestamp,
    val source: EventSource,
    val payload: EventPayload,
    /** この記録を打ち消した Revocation イベントのID。null = 有効 */
    val revokedBy: String? = null,
)
