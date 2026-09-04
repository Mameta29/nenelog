package app.nenelog.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.nenelog.data.db.NenelogDatabase
import app.nenelog.domain.EventPayload
import app.nenelog.domain.EventSource
import app.nenelog.domain.Side
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NursingSessionServiceTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        NenelogDatabase.Schema.create(it)
    }
    private val database = NenelogDatabase(driver)

    @AfterTest
    fun closeDriver() {
        driver.close()
    }

    @Test
    fun session_survives_service_recreation_and_stop_persists_exact_segments() {
        service().start("right", minutes(0), "siri")

        // App Intent / app process の作り直しを模擬。同じDBからActiveSessionを復元する。
        service().start("left", minutes(5), "tap")
        val result = service().stop(minutes(12))

        assertTrue(result.success)
        assertEquals("おしまい。みぎ、5分。ひだり、7分を記録しました", result.responseJa)
        assertEquals("Done. Right 5 minutes and Left 7 minutes recorded.", result.responseEn)
        assertNotNull(result.recordedEventId)

        val store = store()
        assertNull(store.loadActiveSession())
        val event = store.active().single()
        assertEquals(EventSource.SIRI, event.source, "開始経路を記録sourceとして保持する")
        val payload = event.payload as EventPayload.Nursing
        assertEquals(2, payload.segments.size)
        assertEquals(Side.RIGHT, payload.segments[0].side)
        assertEquals(5 * 60_000L, payload.segments[0].durationMillis)
        assertEquals(Side.LEFT, payload.segments[1].side)
        assertEquals(7 * 60_000L, payload.segments[1].durationMillis)
    }

    @Test
    fun duplicate_same_side_start_is_idempotent() {
        val first = service().start("right", minutes(0), "voice_l2")
        val duplicate = service().start("right", minutes(3), "voice_l2")
        val stopped = service().stop(minutes(10))

        assertEquals("右、スタート", first.responseJa)
        assertEquals("右、スタート", duplicate.responseJa)
        assertEquals(10 * 60_000L, stopped.elapsedMillis)
        val payload = store().active().single().payload as EventPayload.Nursing
        assertEquals(1, payload.segments.size)
        assertEquals(10 * 60_000L, payload.segments.single().durationMillis)
    }

    @Test
    fun stop_while_idle_does_not_create_an_event() {
        val result = service().stop(minutes(1))

        assertFalse(result.success)
        assertEquals("タイマーは動いていません", result.responseJa)
        assertEquals("No timer is running.", result.responseEn)
        assertTrue(store().active().isEmpty())
    }

    @Test
    fun timeline_is_read_from_durable_events() {
        service().start("left", minutes(2), "tap")
        service().stop(minutes(8))

        val item = service().timeline().single()
        assertEquals(minutes(2), item.occurredAtEpochMillis)
        assertEquals(minutes(8), item.endedAtEpochMillis)
        assertEquals(6 * 60_000L, item.totalDurationMillis)
        assertEquals(6 * 60_000L, item.leftDurationMillis)
        assertEquals(0, item.rightDurationMillis)
        assertEquals("tap", item.sourceCode)
    }

    @Test
    fun status_after_ninety_minutes_auto_closes_and_persists_the_session() {
        service().start("right", minutes(0), "voice_l2")

        val result = service().status(minutes(95))

        assertEquals("idle", result.stateCode)
        assertEquals(90 * 60_000L, result.elapsedMillis)
        assertNotNull(result.recordedEventId)
        assertNull(store().loadActiveSession())
        val payload = store().active().single().payload as EventPayload.Nursing
        assertTrue(payload.autoClosed)
        assertEquals(90 * 60_000L, payload.totalDurationMillis)
    }

    @Test
    fun starting_after_an_expired_session_saves_the_old_one_then_starts_a_new_one() {
        service().start("left", minutes(0), "tap")

        val result = service().start("right", minutes(120), "voice_l2")

        assertEquals("running", result.stateCode)
        assertEquals("right", result.currentSideCode)
        val oldPayload = store().active().single().payload as EventPayload.Nursing
        assertTrue(oldPayload.autoClosed)
        assertEquals(90 * 60_000L, oldPayload.totalDurationMillis)
        assertEquals(Side.RIGHT, store().loadActiveSession()?.snapshot?.currentSide)
    }

    @Test
    fun undo_appends_a_revocation_without_deleting_the_original_event() {
        service().start("left", minutes(0), "tap")
        service().stop(minutes(4))
        val original = store().active().single()

        val undone = store().undoLast("caregiver-1", app.nenelog.domain.Timestamp(minutes(5)))

        assertEquals(original.id, undone?.id)
        assertTrue(store().active().isEmpty())
        val allEvents = store().all()
        assertEquals(2, allEvents.size)
        val storedOriginal = allEvents.first { it.id == original.id }
        val revocation = allEvents.first { it.payload is EventPayload.Revocation }
        assertEquals(revocation.id, storedOriginal.revokedBy)
        assertEquals(original.id, (revocation.payload as EventPayload.Revocation).targetEventId)
    }

    private fun store() = SqlDelightEventStore(database, "Asia/Tokyo")

    private fun service() = NursingSessionService(
        store = store(),
        babyId = "baby-1",
        caregiverId = "caregiver-1",
        timezoneId = "Asia/Tokyo",
    )

    private fun minutes(value: Int): Long = value * 60_000L
}
