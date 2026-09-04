package app.nenelog.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NursingResponseComposerTest {

    @Test
    fun stop_script_uses_right_then_left_and_both_languages() {
        val payload = EventPayload.Nursing(
            segments = listOf(
                NursingSegment(Side.RIGHT, Timestamp(0), Timestamp(5 * 60_000L)),
                NursingSegment(Side.LEFT, Timestamp(5 * 60_000L), Timestamp(12 * 60_000L)),
            ),
        )

        val response = NursingResponseComposer.stopped(payload)
        assertEquals("おしまい。みぎ、5分。ひだり、7分を記録しました", response.ja)
        assertEquals("Done. Right 5 minutes and Left 7 minutes recorded.", response.en)
    }

    @Test
    fun short_nonzero_segment_is_spoken_as_one_minute() {
        val payload = EventPayload.Nursing(
            segments = listOf(NursingSegment(Side.LEFT, Timestamp(0), Timestamp(20_000))),
        )
        assertEquals("おしまい。ひだり、1分を記録しました", NursingResponseComposer.stopped(payload).ja)
    }
}
