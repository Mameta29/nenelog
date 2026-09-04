package app.nenelog.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VoicePhraseCandidateEvaluatorTest {

    @Test
    fun first_exact_candidate_is_selected_and_kana_kanji_aliases_are_supported() {
        val result = VoicePhraseCandidateEvaluator.evaluate(
            finalCandidates = listOf("右のおぱい", "右 の おっぱい"),
            lastPartialOnNoMatch = null,
            acceptedPhrases = setOf("みぎのおっぱい", "右のおっぱい"),
        )

        assertEquals("右 の おっぱい", result.selectedTranscript)
        assertEquals("右のおっぱい", result.matchedPhrase)
    }

    @Test
    fun exact_last_partial_can_be_selected_after_no_match() {
        val result = VoicePhraseCandidateEvaluator.evaluate(
            finalCandidates = emptyList(),
            lastPartialOnNoMatch = "ひだり、で授乳。",
            acceptedPhrases = setOf("ひだりで授乳", "左で授乳"),
        )

        assertEquals("ひだり、で授乳。", result.selectedTranscript)
        assertEquals("ひだりで授乳", result.matchedPhrase)
    }

    @Test
    fun embedded_or_near_match_is_rejected() {
        val embedded = VoicePhraseCandidateEvaluator.evaluate(
            finalCandidates = listOf("右のおっぱいを始めます"),
            lastPartialOnNoMatch = null,
            acceptedPhrases = setOf("右のおっぱい"),
        )
        val nearMatch = VoicePhraseCandidateEvaluator.evaluate(
            finalCandidates = listOf("右のおぱい"),
            lastPartialOnNoMatch = null,
            acceptedPhrases = setOf("右のおっぱい"),
        )

        assertNull(embedded.selectedTranscript)
        assertNull(embedded.matchedPhrase)
        assertNull(nearMatch.selectedTranscript)
        assertNull(nearMatch.matchedPhrase)
    }
}
