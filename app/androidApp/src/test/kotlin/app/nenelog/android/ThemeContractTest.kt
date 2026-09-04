package app.nenelog.android

import androidx.compose.ui.graphics.Color
import app.nenelog.ui.theme.ThemePreference
import app.nenelog.ui.theme.ThemeVariant
import app.nenelog.ui.theme.linenColorsFor
import app.nenelog.ui.theme.linenMaterialColorScheme
import app.nenelog.ui.theme.resolveThemeVariant
import app.nenelog.ui.model.VoiceUiPhase
import app.nenelog.ui.model.VoiceUiStateStore
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContractTest {
    @Test
    fun autoThemeUsesOledOnlyFrom22Through5() {
        assertEquals(ThemeVariant.DARK, resolveThemeVariant(ThemePreference.AUTO, true, 21))
        assertEquals(ThemeVariant.NIGHT, resolveThemeVariant(ThemePreference.AUTO, false, 22))
        assertEquals(ThemeVariant.NIGHT, resolveThemeVariant(ThemePreference.AUTO, false, 5))
        assertEquals(ThemeVariant.LIGHT, resolveThemeVariant(ThemePreference.AUTO, false, 6))
    }

    @Test
    fun manualThemeAlwaysOverridesClockAndSystem() {
        assertEquals(ThemeVariant.LIGHT, resolveThemeVariant(ThemePreference.LIGHT, true, 23))
        assertEquals(ThemeVariant.DARK, resolveThemeVariant(ThemePreference.DARK, false, 12))
        assertEquals(ThemeVariant.NIGHT, resolveThemeVariant(ThemePreference.NIGHT, false, 12))
    }

    @Test
    fun everyThemeKeepsCoreTextAndButtonRolesAtWcagAaContrast() {
        ThemeVariant.entries.forEach { variant ->
            val linen = linenColorsFor(variant)
            val material = linenMaterialColorScheme(variant)
            assertContrast("$variant body on canvas", linen.ink, linen.canvas, minimum = 4.5)
            assertContrast("$variant muted on canvas", linen.inkMuted, linen.canvas, minimum = 4.5)
            assertContrast("$variant body on surface", linen.ink, linen.surface, minimum = 4.5)
            assertContrast("$variant body on quiet surface", linen.ink, linen.surfaceQuiet, minimum = 4.5)
            assertContrast("$variant primary button", material.onPrimary, material.primary, minimum = 4.5)
            assertContrast("$variant stop button", material.onSecondary, material.secondary, minimum = 4.5)
        }
    }

    @Test
    fun platformVoiceCodesMapToEveryExplicitUiState() {
        val store = VoiceUiStateStore()
        val expectations = listOf(
            "waiting" to VoiceUiPhase.WAITING,
            "listening" to VoiceUiPhase.LISTENING,
            "recognized" to VoiceUiPhase.RECOGNIZED,
            "responding" to VoiceUiPhase.RESPONDING,
            "failure" to VoiceUiPhase.FAILURE,
        )

        expectations.forEach { (code, phase) ->
            store.update(code, "right start")
            assertEquals(phase, store.status.phase)
            assertEquals("right start", store.status.transcript)
        }
    }

    private fun assertContrast(label: String, foreground: Color, background: Color, minimum: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$label contrast was $ratio, expected >= $minimum", ratio >= minimum)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = max(luminance(first), luminance(second))
        val darker = min(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linearize(color.red.toDouble()) +
            0.7152 * linearize(color.green.toDouble()) +
            0.0722 * linearize(color.blue.toDouble())

    private fun linearize(value: Double): Double =
        if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}
