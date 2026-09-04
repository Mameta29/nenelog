package app.nenelog.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

/**
 * タイマーは信頼の核(docs/04)— ここに最も厚くテストを書く。
 * 時刻は全て注入なので実時間に依存しない。
 */
class NursingSessionStateMachineTest {

    private val baby = "baby-1"
    private fun t(minutes: Int, seconds: Int = 0) = Timestamp(minutes * 60_000L + seconds * 1000L)

    // --- 基本遷移 ---

    @Test
    fun idle_start_becomes_running_on_that_side() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        val s = m.state.shouldBeInstanceOf<NursingSessionStateMachine.State.Running>()
        s.side shouldBe Side.RIGHT
        s.startedAt shouldBe t(0)
    }

    @Test
    fun stop_produces_single_segment_with_correct_duration() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        val payload = m.stop(t(12))
        payload.segments.size shouldBe 1
        payload.segments[0] shouldBe NursingSegment(Side.RIGHT, t(0), t(12))
        payload.totalDurationMillis shouldBe 12 * 60_000L
        payload.autoClosed.shouldBeFalse()
        m.state.shouldBeInstanceOf<NursingSessionStateMachine.State.Completed>()
    }

    @Test
    fun start_when_running_throws() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.LEFT, t(0))
        shouldThrow<IllegalStateException> { m.start(baby, Side.RIGHT, t(1)) }
    }

    @Test
    fun stop_when_idle_throws() {
        shouldThrow<IllegalStateException> { NursingSessionStateMachine().stop(t(1)) }
    }

    @Test
    fun pause_when_idle_throws() {
        shouldThrow<IllegalStateException> { NursingSessionStateMachine().pause(t(1)) }
    }

    @Test
    fun resume_when_running_throws() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.LEFT, t(0))
        shouldThrow<IllegalStateException> { m.resume(t(1)) }
    }

    // --- 左右切替 ---

    @Test
    fun switch_side_creates_two_segments() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        m.switchSide(Side.LEFT, t(7)).shouldBeTrue()
        val payload = m.stop(t(15))
        payload.segments shouldBe listOf(
            NursingSegment(Side.RIGHT, t(0), t(7)),
            NursingSegment(Side.LEFT, t(7), t(15)),
        )
    }

    @Test
    fun switch_to_same_side_is_noop_for_duplicate_voice_command() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        m.switchSide(Side.RIGHT, t(3)).shouldBeFalse()
        m.stop(t(10)).segments.size shouldBe 1
    }

    // --- 一時停止 ---

    @Test
    fun paused_time_is_excluded_from_elapsed() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.LEFT, t(0))
        m.pause(t(5))
        m.elapsedMillis(t(20)) shouldBe 5 * 60_000L // pause中の15分は数えない
        m.resume(t(20))
        m.elapsedMillis(t(23)) shouldBe 8 * 60_000L
        val payload = m.stop(t(25))
        payload.segments shouldBe listOf(
            NursingSegment(Side.LEFT, t(0), t(5)),
            NursingSegment(Side.LEFT, t(20), t(25)),
        )
    }

    @Test
    fun resume_with_explicit_side_switches() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.LEFT, t(0))
        m.pause(t(5))
        m.resume(t(10), Side.RIGHT)
        val s = m.state.shouldBeInstanceOf<NursingSessionStateMachine.State.Running>()
        s.side shouldBe Side.RIGHT
    }

    @Test
    fun stop_while_paused_uses_only_completed_segments() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        m.pause(t(9))
        val payload = m.stop(t(30))
        payload.segments shouldBe listOf(NursingSegment(Side.RIGHT, t(0), t(9)))
    }

    // --- 「今何分?」応答用 ---

    @Test
    fun current_side_elapsed_reflects_only_running_segment() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        m.switchSide(Side.LEFT, t(7))
        m.currentSideElapsedMillis(t(12)) shouldBe 5 * 60_000L
        m.elapsedMillis(t(12)) shouldBe 12 * 60_000L
    }

    // --- 90分自動クローズ ---

    @Test
    fun tick_before_deadline_returns_null() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.LEFT, t(0))
        m.tick(t(89)).shouldBeNull()
        m.state.shouldBeInstanceOf<NursingSessionStateMachine.State.Running>()
    }

    @Test
    fun tick_after_deadline_auto_closes_and_clamps_segment_end() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.LEFT, t(0))
        val payload = m.tick(t(200)).shouldNotBeNull()
        payload.autoClosed.shouldBeTrue()
        payload.segments shouldBe listOf(NursingSegment(Side.LEFT, t(0), t(90)))
        m.state.shouldBeInstanceOf<NursingSessionStateMachine.State.Completed>()
    }

    @Test
    fun tick_when_paused_past_deadline_completes_with_existing_segments() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        m.pause(t(10))
        val payload = m.tick(t(95)).shouldNotBeNull()
        payload.autoClosed.shouldBeTrue()
        payload.segments shouldBe listOf(NursingSegment(Side.RIGHT, t(0), t(10)))
    }

    @Test
    fun custom_auto_close_duration_is_respected() {
        val m = NursingSessionStateMachine(autoCloseAfterMillis = 10 * 60_000L)
        m.start(baby, Side.LEFT, t(0))
        m.tick(t(9)).shouldBeNull()
        m.tick(t(10)).shouldNotBeNull()
    }

    // --- プロセス再起動からの復元(docs/04: タイマー=開始時刻の記録) ---

    @Test
    fun restore_running_session_continues_elapsed_from_wall_clock() {
        val m1 = NursingSessionStateMachine()
        m1.start(baby, Side.RIGHT, t(0))
        m1.switchSide(Side.LEFT, t(6))
        val snapshot = m1.snapshot().shouldNotBeNull()

        // プロセスキル → 再起動
        val m2 = NursingSessionStateMachine.restore(snapshot)
        m2.elapsedMillis(t(10)) shouldBe 10 * 60_000L
        val payload = m2.stop(t(11))
        payload.segments shouldBe listOf(
            NursingSegment(Side.RIGHT, t(0), t(6)),
            NursingSegment(Side.LEFT, t(6), t(11)),
        )
    }

    @Test
    fun restore_paused_session_stays_paused() {
        val m1 = NursingSessionStateMachine()
        m1.start(baby, Side.LEFT, t(0))
        m1.pause(t(4))
        val snapshot = m1.snapshot().shouldNotBeNull()

        val m2 = NursingSessionStateMachine.restore(snapshot)
        m2.state.shouldBeInstanceOf<NursingSessionStateMachine.State.Paused>()
        m2.elapsedMillis(t(60)) shouldBe 4 * 60_000L
        m2.resume(t(60))
        m2.stop(t(62)).totalDurationMillis shouldBe 6 * 60_000L
    }

    @Test
    fun restore_then_tick_auto_closes_overdue_session() {
        // 深夜に開始したまま寝落ち → 翌朝アプリ復帰、が最重要の実ケース
        val m1 = NursingSessionStateMachine()
        m1.start(baby, Side.RIGHT, t(0))
        val snapshot = m1.snapshot().shouldNotBeNull()

        val m2 = NursingSessionStateMachine.restore(snapshot)
        val payload = m2.tick(t(8 * 60)).shouldNotBeNull() // 8時間後
        payload.autoClosed.shouldBeTrue()
        payload.segments shouldBe listOf(NursingSegment(Side.RIGHT, t(0), t(90)))
    }

    @Test
    fun snapshot_is_null_when_idle_or_completed() {
        val m = NursingSessionStateMachine()
        m.snapshot().shouldBeNull()
        m.start(baby, Side.LEFT, t(0))
        m.stop(t(5))
        m.snapshot().shouldBeNull()
    }

    // --- 境界値 ---

    @Test
    fun zero_length_segments_are_dropped() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        m.switchSide(Side.LEFT, t(0)) // 開始直後に切替
        val payload = m.stop(t(5))
        payload.segments shouldBe listOf(NursingSegment(Side.LEFT, t(0), t(5)))
    }

    @Test
    fun immediate_stop_produces_empty_segments() {
        val m = NursingSessionStateMachine()
        m.start(baby, Side.RIGHT, t(0))
        val payload = m.stop(t(0))
        payload.segments shouldBe emptyList()
        payload.totalDurationMillis shouldBe 0L
    }
}
