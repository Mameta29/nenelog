package app.nenelog.domain

/**
 * Androidのオンデバイス認識が最終結果を返さずNO_MATCHで閉じた場合の限定フォールバック。
 * 到着済みの最後の途中候補だけを対象にし、固定文法への完全一致は既存Interpreterへ委ねる。
 */
object VoiceRecognitionFallback {
    fun selectLastPartialOnNoMatch(lastPartial: String?, locale: String): String? =
        lastPartial
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { VoiceCommandInterpreter.interpret(it, locale) != null }
}
