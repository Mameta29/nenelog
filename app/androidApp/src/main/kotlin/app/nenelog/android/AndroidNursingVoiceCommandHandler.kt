package app.nenelog.android

import app.nenelog.data.NursingSessionService
import app.nenelog.android.spike.RecognitionReply
import app.nenelog.domain.Side
import app.nenelog.domain.VoiceCommand
import app.nenelog.domain.VoiceCommandInterpreter

/** Pixel L2: 認識文字列を固定文法で解釈し、DB更新後の短いTTS応答を返す。 */
class AndroidNursingVoiceCommandHandler(
    private val service: NursingSessionService,
) {
    fun handle(transcript: String, locale: String, epochMillis: Long): RecognitionReply? {
        val command = VoiceCommandInterpreter.interpret(transcript, locale) ?: return null
        val result = when (command) {
            is VoiceCommand.StartNursing -> service.start(
                sideCode = if (command.side == Side.RIGHT) "right" else "left",
                epochMillis = epochMillis,
                sourceCode = NursingSessionService.SOURCE_VOICE_L2,
            )
            VoiceCommand.StopNursing -> service.stop(epochMillis)
        }
        return RecognitionReply(
            spokenText = if (locale.startsWith("ja", ignoreCase = true)) {
                result.responseJa
            } else {
                result.responseEn
            },
            endSessionAfterSpeaking = command is VoiceCommand.StopNursing,
        )
    }
}
