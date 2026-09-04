package app.nenelog.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VoiceRecognitionFallbackTest {

    @Test
    fun last_partial_is_accepted_only_when_it_matches_the_fixed_grammar() {
        assertEquals(
            "右",
            VoiceRecognitionFallback.selectLastPartialOnNoMatch("右", "ja-JP"),
        )
        assertEquals(
            "stop",
            VoiceRecognitionFallback.selectLastPartialOnNoMatch("stop", "en-US"),
        )
    }

    @Test
    fun blank_embedded_and_unrelated_partials_are_rejected() {
        assertNull(VoiceRecognitionFallback.selectLastPartialOnNoMatch(null, "ja-JP"))
        assertNull(VoiceRecognitionFallback.selectLastPartialOnNoMatch("  ", "ja-JP"))
        assertNull(
            VoiceRecognitionFallback.selectLastPartialOnNoMatch(
                "右スタートをお願いします",
                "ja-JP",
            ),
        )
        assertNull(
            VoiceRecognitionFallback.selectLastPartialOnNoMatch(
                "職場のパソコンとした時",
                "ja-JP",
            ),
        )
    }
}
