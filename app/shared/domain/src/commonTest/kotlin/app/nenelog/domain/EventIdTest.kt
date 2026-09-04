package app.nenelog.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlin.random.Random
import kotlin.test.Test

class EventIdTest {

    @Test
    fun format_is_canonical_uuid() {
        val id = EventId.new(Timestamp(1_755_200_000_000))
        id shouldMatch Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    }

    @Test
    fun ids_sort_lexicographically_by_timestamp() {
        val earlier = EventId.new(Timestamp(1_000_000_000_000))
        val later = EventId.new(Timestamp(1_000_000_000_001))
        (earlier < later) shouldBe true
    }

    @Test
    fun timestamp_prefix_encodes_epoch_millis() {
        val millis = 0x0123456789ABL
        val id = EventId.new(Timestamp(millis), random = Random(42))
        id.substring(0, 8) + id.substring(9, 13) shouldBe "0123456789ab"
    }
}
