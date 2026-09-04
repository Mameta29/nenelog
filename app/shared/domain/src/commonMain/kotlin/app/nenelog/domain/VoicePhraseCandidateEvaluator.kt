package app.nenelog.domain

data class VoicePhraseCandidateEvaluation(
    val selectedTranscript: String?,
    val matchedPhrase: String?,
)

/**
 * R7の候補語彙を製品文法へ追加せず、完全一致だけで比較するための純粋関数。
 * 誤変換を部分一致で成功扱いしない。
 */
object VoicePhraseCandidateEvaluator {
    fun evaluate(
        finalCandidates: List<String>,
        lastPartialOnNoMatch: String?,
        acceptedPhrases: Set<String>,
    ): VoicePhraseCandidateEvaluation {
        val acceptedByNormalized = acceptedPhrases.associateBy(::normalize)

        finalCandidates.forEach { transcript ->
            val matchedPhrase = acceptedByNormalized[normalize(transcript)]
            if (matchedPhrase != null) {
                return VoicePhraseCandidateEvaluation(
                    selectedTranscript = transcript,
                    matchedPhrase = matchedPhrase,
                )
            }
        }

        val partial = lastPartialOnNoMatch?.takeIf { it.isNotBlank() }
        val matchedPartial = partial?.let { acceptedByNormalized[normalize(it)] }
        return VoicePhraseCandidateEvaluation(
            selectedTranscript = partial.takeIf { matchedPartial != null },
            matchedPhrase = matchedPartial,
        )
    }

    private fun normalize(transcript: String): String = transcript
        .trim()
        .lowercase()
        .filterNot { character ->
            character.isWhitespace() || character in PHRASE_PUNCTUATION
        }

    private val PHRASE_PUNCTUATION = setOf('。', '、', '.', ',', '!', '！', '?', '？')
}
