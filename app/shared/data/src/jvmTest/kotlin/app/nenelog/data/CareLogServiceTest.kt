package app.nenelog.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.nenelog.data.db.NenelogDatabase
import app.nenelog.domain.EventPayload
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CareLogServiceTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        NenelogDatabase.Schema.create(it)
    }
    private val database = NenelogDatabase(driver)
    private val store = SqlDelightEventStore(database, "Asia/Tokyo")
    private val service = CareLogService(store, babyId = "baby-1", caregiverId = "caregiver-1")

    @AfterTest
    fun closeDriver() {
        driver.close()
    }

    @Test
    fun every_supported_tap_record_is_written_to_the_shared_event_timeline() {
        service.recordBottle(80, "formula", minutes(1))
        service.recordPumping("left", 40, 12, minutes(2))
        service.recordDiaper(pee = true, poop = true, amountCode = "m", epochMillis = minutes(3))
        service.recordSleep(durationMinutes = 90, endedAtEpochMillis = minutes(100))
        service.recordTemperature(36.8, minutes(101))
        service.recordMedicine("Vitamin D", null, minutes(102))
        service.recordBath(minutes(103))
        service.recordMemo("First smile", minutes(104))
        service.recordGrowth(weightG = 4_200, heightMm = null, epochMillis = minutes(105))

        val timeline = service.timeline()

        assertEquals(9, timeline.size)
        assertIs<EventPayload.Growth>(timeline[0].payload)
        assertIs<EventPayload.Memo>(timeline[1].payload)
        assertIs<EventPayload.Bath>(timeline[2].payload)
        assertIs<EventPayload.Medicine>(timeline[3].payload)
        assertIs<EventPayload.Temperature>(timeline[4].payload)
        assertIs<EventPayload.Sleep>(timeline[5].payload)
        assertIs<EventPayload.Diaper>(timeline[6].payload)
        assertIs<EventPayload.Pumping>(timeline[7].payload)
        assertIs<EventPayload.Bottle>(timeline[8].payload)
    }

    @Test
    fun summary_uses_exact_persisted_values_inside_the_requested_range() {
        val nursing = NursingSessionService(store)
        nursing.start("left", minutes(10), "tap")
        nursing.stop(minutes(18))
        service.recordBottle(90, "breast_milk", minutes(20))
        service.recordBottle(60, "formula", minutes(30))
        service.recordDiaper(pee = true, poop = false, amountCode = null, epochMillis = minutes(40))
        service.recordDiaper(pee = false, poop = true, amountCode = "s", epochMillis = minutes(50))
        service.recordSleep(durationMinutes = 30, endedAtEpochMillis = minutes(80))
        service.recordMemo("outside", minutes(200))

        val summary = service.summary(minutes(0), minutes(100))

        assertEquals(1, summary.nursingCount)
        assertEquals(minutes(8), summary.nursingDurationMillis)
        assertEquals(2, summary.bottleCount)
        assertEquals(150, summary.bottleAmountMl)
        assertEquals(2, summary.diaperCount)
        assertEquals(1, summary.poopCount)
        assertEquals(minutes(30), summary.sleepDurationMillis)
        assertEquals(0, summary.memoCount)
        assertEquals(3, summary.feedingCount)
        assertTrue(!summary.isEmpty)
    }

    @Test
    fun undo_revokes_the_latest_record_without_deleting_history() {
        val first = service.recordBath(minutes(1))
        val latest = service.recordMemo("remember this", minutes(2))

        val undone = service.undoLast(minutes(3))

        assertEquals(latest.id, undone?.id)
        assertEquals(listOf(first.id), service.timeline().map { it.id })
        assertEquals(3, store.all().size)
    }

    @Test
    fun invalid_manual_values_are_rejected_before_an_event_is_written() {
        assertFailsWith<IllegalArgumentException> {
            service.recordBottle(0, "formula", minutes(1))
        }
        assertFailsWith<IllegalArgumentException> {
            service.recordDiaper(false, false, null, minutes(1))
        }
        assertFailsWith<IllegalArgumentException> {
            service.recordMemo("   ", minutes(1))
        }

        assertTrue(service.timeline().isEmpty())
    }

    @Test
    fun theme_choice_is_persisted_and_unknown_values_fall_back_to_auto() {
        val settings = AppSettingsService(SqlDelightAppSettingsStore(database))
        assertEquals(AppSettingsService.THEME_AUTO, settings.themePreferenceCode())

        settings.setThemePreferenceCode(AppSettingsService.THEME_NIGHT)

        val recreated = AppSettingsService(SqlDelightAppSettingsStore(database))
        assertEquals(AppSettingsService.THEME_NIGHT, recreated.themePreferenceCode())
        assertFailsWith<IllegalArgumentException> {
            recreated.setThemePreferenceCode("neon")
        }
    }

    private fun minutes(value: Int): Long = value * 60_000L
}
