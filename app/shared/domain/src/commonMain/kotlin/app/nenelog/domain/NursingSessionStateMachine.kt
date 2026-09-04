package app.nenelog.domain

import kotlinx.serialization.Serializable

/**
 * プロセス再起動をまたぐ復元用スナップショット(docs/04)。
 * "走っているタイマー" とは開始時刻の記録のこと — 経過時間は常に実時刻差分で計算する。
 */
@Serializable
data class NursingSessionSnapshot(
    val babyId: String,
    val paused: Boolean,
    val currentSide: Side,
    val completedSegments: List<NursingSegment>,
    val startedAt: Timestamp,
    /** Running 中のみ非null: 現在セグメントの開始時刻 */
    val currentSegmentStartedAt: Timestamp?,
)

/**
 * 授乳タイマーの唯一の真実(docs/04)。
 * idle → running(L|R) → paused → completed。副作用なし・時刻は全メソッドで注入。
 */
class NursingSessionStateMachine(
    val autoCloseAfterMillis: Long = DEFAULT_AUTO_CLOSE_MILLIS,
) {

    sealed interface State {
        data object Idle : State

        data class Running(
            val babyId: String,
            val side: Side,
            val startedAt: Timestamp,
            val currentSegmentStartedAt: Timestamp,
            val completedSegments: List<NursingSegment>,
        ) : State

        data class Paused(
            val babyId: String,
            val lastSide: Side,
            val startedAt: Timestamp,
            val completedSegments: List<NursingSegment>,
        ) : State

        data class Completed(
            val babyId: String,
            val payload: EventPayload.Nursing,
        ) : State
    }

    var state: State = State.Idle
        private set

    /** 「右スタート」: idle からのみ */
    fun start(babyId: String, side: Side, at: Timestamp) {
        check(state is State.Idle) { "cannot start: session already ${state::class.simpleName}" }
        state = State.Running(
            babyId = babyId,
            side = side,
            startedAt = at,
            currentSegmentStartedAt = at,
            completedSegments = emptyList(),
        )
    }

    /**
     * 「左」「右」での切替。同じ側なら no-op(音声コマンドの重複発話に対して冪等)。
     * 戻り値: 実際に切り替わったか。
     */
    fun switchSide(side: Side, at: Timestamp): Boolean {
        val s = requireRunning("switchSide")
        if (s.side == side) return false
        state = s.copy(
            side = side,
            currentSegmentStartedAt = at,
            completedSegments = s.completedSegments + s.closeCurrentSegment(at),
        )
        return true
    }

    fun pause(at: Timestamp) {
        val s = requireRunning("pause")
        state = State.Paused(
            babyId = s.babyId,
            lastSide = s.side,
            startedAt = s.startedAt,
            completedSegments = s.completedSegments + s.closeCurrentSegment(at),
        )
    }

    /** 再開。side 省略時は直前の側で再開 */
    fun resume(at: Timestamp, side: Side? = null) {
        val s = state as? State.Paused ?: error("cannot resume: session is ${state::class.simpleName}")
        state = State.Running(
            babyId = s.babyId,
            side = side ?: s.lastSide,
            startedAt = s.startedAt,
            currentSegmentStartedAt = at,
            completedSegments = s.completedSegments,
        )
    }

    /** 「ストップ」: 記録として確定し payload を返す */
    fun stop(at: Timestamp, note: String? = null): EventPayload.Nursing {
        val (babyId, payload) = when (val s = state) {
            is State.Running -> s.babyId to EventPayload.Nursing(
                segments = (s.completedSegments + s.closeCurrentSegment(at)).dropZeroLength(),
                note = note,
            )
            is State.Paused -> s.babyId to EventPayload.Nursing(
                segments = s.completedSegments.dropZeroLength(),
                note = note,
            )
            else -> error("cannot stop: session is ${s::class.simpleName}")
        }
        state = State.Completed(babyId, payload)
        return payload
    }

    /** 「今何分?」: 授乳時間の合計(pause 中の時間は含まない) */
    fun elapsedMillis(at: Timestamp): Long = when (val s = state) {
        is State.Running -> s.completedSegments.sumOf { it.durationMillis } + (at - s.currentSegmentStartedAt)
        is State.Paused -> s.completedSegments.sumOf { it.durationMillis }
        is State.Completed -> s.payload.totalDurationMillis
        State.Idle -> 0L
    }

    /** 現在走っている側の連続時間(TTS応答「右side、12分です」用) */
    fun currentSideElapsedMillis(at: Timestamp): Long = when (val s = state) {
        is State.Running -> at - s.currentSegmentStartedAt
        else -> 0L
    }

    /**
     * 定期チェック(またはアプリ復帰時)に呼ぶ。開始から autoCloseAfterMillis を
     * 超えていたら自動クローズし payload を返す(docs/04: 90分自動クローズ)。
     * セグメント終端は締切時刻でクランプする。
     */
    fun tick(at: Timestamp): EventPayload.Nursing? {
        val deadline = when (val s = state) {
            is State.Running -> s.startedAt.plusMillis(autoCloseAfterMillis)
            is State.Paused -> s.startedAt.plusMillis(autoCloseAfterMillis)
            else -> return null
        }
        if (at < deadline) return null

        val payload = when (val s = state) {
            is State.Running -> EventPayload.Nursing(
                segments = (s.completedSegments + s.closeCurrentSegment(deadline)).dropZeroLength(),
                autoClosed = true,
            )
            is State.Paused -> EventPayload.Nursing(
                segments = s.completedSegments.dropZeroLength(),
                autoClosed = true,
            )
            else -> return null
        }
        val babyId = when (val s = state) {
            is State.Running -> s.babyId
            is State.Paused -> s.babyId
            else -> return null
        }
        state = State.Completed(babyId, payload)
        return payload
    }

    fun snapshot(): NursingSessionSnapshot? = when (val s = state) {
        is State.Running -> NursingSessionSnapshot(
            babyId = s.babyId,
            paused = false,
            currentSide = s.side,
            completedSegments = s.completedSegments,
            startedAt = s.startedAt,
            currentSegmentStartedAt = s.currentSegmentStartedAt,
        )
        is State.Paused -> NursingSessionSnapshot(
            babyId = s.babyId,
            paused = true,
            currentSide = s.lastSide,
            completedSegments = s.completedSegments,
            startedAt = s.startedAt,
            currentSegmentStartedAt = null,
        )
        else -> null
    }

    private fun requireRunning(op: String): State.Running =
        state as? State.Running ?: error("cannot $op: session is ${state::class.simpleName}")

    private fun State.Running.closeCurrentSegment(at: Timestamp) =
        NursingSegment(side = side, startedAt = currentSegmentStartedAt, endedAt = at)

    private fun List<NursingSegment>.dropZeroLength() = filter { it.durationMillis > 0 }

    companion object {
        const val DEFAULT_AUTO_CLOSE_MILLIS: Long = 90L * 60 * 1000

        /** プロセス再起動からの復元(docs/04) */
        fun restore(
            snapshot: NursingSessionSnapshot,
            autoCloseAfterMillis: Long = DEFAULT_AUTO_CLOSE_MILLIS,
        ): NursingSessionStateMachine {
            val machine = NursingSessionStateMachine(autoCloseAfterMillis)
            machine.state = if (snapshot.paused) {
                State.Paused(
                    babyId = snapshot.babyId,
                    lastSide = snapshot.currentSide,
                    startedAt = snapshot.startedAt,
                    completedSegments = snapshot.completedSegments,
                )
            } else {
                State.Running(
                    babyId = snapshot.babyId,
                    side = snapshot.currentSide,
                    startedAt = snapshot.startedAt,
                    currentSegmentStartedAt = requireNotNull(snapshot.currentSegmentStartedAt) {
                        "running snapshot must have currentSegmentStartedAt"
                    },
                    completedSegments = snapshot.completedSegments,
                )
            }
            return machine
        }
    }
}
