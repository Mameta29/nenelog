package app.nenelog.integration

import app.nenelog.data.NursingSessionService
import app.nenelog.domain.Side
import app.nenelog.domain.VoiceCommand
import app.nenelog.domain.VoiceCommandInterpreter

/** Swift App Intents向け。Kotlin value classを跨がずprimitiveだけを受け取る。 */
class AppIntentDomainBridge {

    fun startNursingResponse(
        sideCode: String,
        epochMillis: Long,
        localeCode: String,
    ): String = IosAppGraph.nursing.start(
            sideCode = sideCode,
            epochMillis = epochMillis,
            sourceCode = NursingSessionService.SOURCE_SIRI,
        ).localizedFor(localeCode)

    fun stopNursingResponse(epochMillis: Long, localeCode: String): String =
        IosAppGraph.nursing.stop(epochMillis).localizedFor(localeCode)

    /** iOS L2用。固定文法に合う単独発話だけDB更新+TTS応答を返す。 */
    fun handleVoiceCommandResponse(
        transcript: String,
        epochMillis: Long,
        localeCode: String,
    ): String? {
        val command = VoiceCommandInterpreter.interpret(transcript, localeCode) ?: return null
        val result = when (command) {
            is VoiceCommand.StartNursing -> IosAppGraph.nursing.start(
                sideCode = if (command.side == Side.RIGHT) "right" else "left",
                epochMillis = epochMillis,
                sourceCode = NursingSessionService.SOURCE_VOICE_L2,
            )
            VoiceCommand.StopNursing -> IosAppGraph.nursing.stop(epochMillis)
        }
        return result.localizedFor(localeCode)
    }

    fun isStopVoiceCommand(transcript: String, localeCode: String): Boolean =
        VoiceCommandInterpreter.interpret(transcript, localeCode) is VoiceCommand.StopNursing
}

private fun app.nenelog.data.NursingCommandResult.localizedFor(localeCode: String): String =
    if (localeCode.startsWith("ja", ignoreCase = true)) responseJa else responseEn
