package app.nenelog.domain

/** P0固定文法。文中の単語拾いはせず、単独発話だけを受理する(docs/05)。 */
sealed interface VoiceCommand {
    data class StartNursing(val side: Side) : VoiceCommand
    data object StopNursing : VoiceCommand
}

object VoiceCommandInterpreter {

    fun interpret(transcript: String, locale: String): VoiceCommand? {
        val normalized = transcript
            .trim()
            .lowercase()
            .filterNot { character ->
                character.isWhitespace() || character in COMMAND_PUNCTUATION
            }

        return if (locale.startsWith("ja", ignoreCase = true)) {
            when (normalized) {
                "右", "右スタート" -> VoiceCommand.StartNursing(Side.RIGHT)
                "左", "左スタート" -> VoiceCommand.StartNursing(Side.LEFT)
                "ストップ", "終わり", "ごちそうさま" -> VoiceCommand.StopNursing
                else -> null
            }
        } else {
            when (normalized) {
                "right", "rightstart", "startright" -> VoiceCommand.StartNursing(Side.RIGHT)
                "left", "leftstart", "startleft" -> VoiceCommand.StartNursing(Side.LEFT)
                "stop", "done" -> VoiceCommand.StopNursing
                else -> null
            }
        }
    }

    private val COMMAND_PUNCTUATION = setOf('。', '、', '.', ',', '!', '！', '?', '？')
}
