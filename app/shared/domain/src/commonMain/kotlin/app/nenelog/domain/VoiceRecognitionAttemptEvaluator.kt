package app.nenelog.domain

data class VoiceRecognitionAttemptEvaluation(
    val selectedTranscript: String?,
    val command: VoiceCommand?,
)

/** ProductionとR7診断で同じ候補選択規則を使うための純粋関数。 */
object VoiceRecognitionAttemptEvaluator {
    fun evaluate(
        finalCandidates: List<String>,
        lastPartialOnNoMatch: String?,
        locale: String,
    ): VoiceRecognitionAttemptEvaluation {
        finalCandidates.forEach { transcript ->
            val command = VoiceCommandInterpreter.interpret(transcript, locale)
            if (command != null) {
                return VoiceRecognitionAttemptEvaluation(transcript, command)
            }
        }

        val fallback = VoiceRecognitionFallback.selectLastPartialOnNoMatch(
            lastPartial = lastPartialOnNoMatch,
            locale = locale,
        )
        return VoiceRecognitionAttemptEvaluation(
            selectedTranscript = fallback,
            command = fallback?.let { VoiceCommandInterpreter.interpret(it, locale) },
        )
    }
}
