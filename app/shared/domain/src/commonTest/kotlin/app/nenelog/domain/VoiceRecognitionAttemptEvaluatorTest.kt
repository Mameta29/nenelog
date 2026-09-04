package app.nenelog.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VoiceRecognitionAttemptEvaluatorTest {

    @Test
    fun first_fixed_grammar_match_is_selected_from_final_candidates() {
        val result = VoiceRecognitionAttemptEvaluator.evaluate(
            finalCandidates = listOf("ミニスタート", "右スタート", "右側です"),
            lastPartialOnNoMatch = null,
            locale = "ja-JP",
        )

        assertEquals("右スタート", result.selectedTranscript)
        assertEquals(VoiceCommand.StartNursing(Side.RIGHT), result.command)
    }

    @Test
    fun exact_last_partial_is_used_only_when_no_final_command_matches() {
        val result = VoiceRecognitionAttemptEvaluator.evaluate(
            finalCandidates = emptyList(),
            lastPartialOnNoMatch = "ストップ",
            locale = "ja-JP",
        )

        assertEquals("ストップ", result.selectedTranscript)
        assertEquals(VoiceCommand.StopNursing, result.command)
    }

    @Test
    fun unrelated_and_embedded_speech_is_not_scored_as_a_command() {
        val result = VoiceRecognitionAttemptEvaluator.evaluate(
            finalCandidates = listOf("職場のパソコンとした時"),
            lastPartialOnNoMatch = "右スタートをお願いします",
            locale = "ja-JP",
        )

        assertNull(result.selectedTranscript)
        assertNull(result.command)
    }
}
