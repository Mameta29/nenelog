package app.nenelog.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class VoiceLocaleResolverTest {

    @Test
    fun normalizes_english_with_japan_region_and_unicode_extension() {
        assertEquals(
            VoiceLocaleResolver.ENGLISH,
            VoiceLocaleResolver.fromLanguageTag("en-JP-u-mu-celsius"),
        )
    }

    @Test
    fun preserves_supported_japanese_locale() {
        assertEquals(
            VoiceLocaleResolver.JAPANESE,
            VoiceLocaleResolver.fromLanguageTag("ja-JP"),
        )
    }

    @Test
    fun falls_back_to_english_for_an_unsupported_language() {
        assertEquals(
            VoiceLocaleResolver.ENGLISH,
            VoiceLocaleResolver.fromLanguageTag("fr-FR"),
        )
    }
}
