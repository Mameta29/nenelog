package app.nenelog.domain

/** Converts an app/device language tag to one of Nenelog's supported speech locales. */
object VoiceLocaleResolver {
    const val ENGLISH = "en-US"
    const val JAPANESE = "ja-JP"

    fun fromLanguageTag(languageTag: String): String {
        val language = languageTag
            .trim()
            .substringBefore('-')
            .substringBefore('_')
            .lowercase()

        return when (language) {
            "ja" -> JAPANESE
            else -> ENGLISH
        }
    }
}
