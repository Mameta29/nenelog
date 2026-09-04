package app.nenelog.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VoiceCommandInterpreterTest {

    @Test
    fun japanese_fixed_grammar_is_interpreted() {
        assertEquals(
            VoiceCommand.StartNursing(Side.RIGHT),
            VoiceCommandInterpreter.interpret("右スタート", "ja-JP"),
        )
        assertEquals(
            VoiceCommand.StartNursing(Side.LEFT),
            VoiceCommandInterpreter.interpret(" 左。 ", "ja-JP"),
        )
        assertEquals(
            VoiceCommand.StartNursing(Side.RIGHT),
            VoiceCommandInterpreter.interpret("右、スタート。", "ja-JP"),
        )
        assertEquals(
            VoiceCommand.StopNursing,
            VoiceCommandInterpreter.interpret("ごちそうさま", "ja-JP"),
        )
    }

    @Test
    fun english_fixed_grammar_is_interpreted() {
        assertEquals(
            VoiceCommand.StartNursing(Side.RIGHT),
            VoiceCommandInterpreter.interpret("Right start", "en-US"),
        )
        assertEquals(
            VoiceCommand.StopNursing,
            VoiceCommandInterpreter.interpret("STOP!", "en-US"),
        )
    }

    @Test
    fun embedded_command_is_rejected_to_reduce_tv_false_positives() {
        assertNull(VoiceCommandInterpreter.interpret("右スタートお願いします", "ja-JP"))
        assertNull(VoiceCommandInterpreter.interpret("please start right now", "en-US"))
        assertNull(VoiceCommandInterpreter.interpret("授乳を始める", "ja-JP"))
    }
}
