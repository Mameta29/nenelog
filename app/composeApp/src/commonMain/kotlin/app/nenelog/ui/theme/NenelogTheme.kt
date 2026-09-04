package app.nenelog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nenelog.data.AppSettingsService

enum class ThemePreference(val storageCode: String) {
    AUTO(AppSettingsService.THEME_AUTO),
    LIGHT(AppSettingsService.THEME_LIGHT),
    DARK(AppSettingsService.THEME_DARK),
    NIGHT(AppSettingsService.THEME_NIGHT),
    ;

    companion object {
        fun fromStorageCode(code: String): ThemePreference =
            entries.firstOrNull { it.storageCode == code } ?: AUTO
    }
}

enum class ThemeVariant { LIGHT, DARK, NIGHT }

fun resolveThemeVariant(
    preference: ThemePreference,
    systemDark: Boolean,
    localHour: Int,
): ThemeVariant = when (preference) {
    ThemePreference.LIGHT -> ThemeVariant.LIGHT
    ThemePreference.DARK -> ThemeVariant.DARK
    ThemePreference.NIGHT -> ThemeVariant.NIGHT
    ThemePreference.AUTO -> when {
        localHour >= 22 || localHour < 6 -> ThemeVariant.NIGHT
        systemDark -> ThemeVariant.DARK
        else -> ThemeVariant.LIGHT
    }
}

@Immutable
data class LinenColors(
    val canvas: Color,
    val surface: Color,
    val surfaceQuiet: Color,
    val ink: Color,
    val inkMuted: Color,
    val sageStrong: Color,
    val sage: Color,
    val sageSoft: Color,
    val terracottaStrong: Color,
    val terracotta: Color,
    val divider: Color,
    val isNight: Boolean,
)

private val LightLinenColors = LinenColors(
    canvas = Color(0xFFFAF6F0),
    surface = Color(0xFFFFFDF9),
    surfaceQuiet = Color(0xFFF1ECE4),
    ink = Color(0xFF213229),
    inkMuted = Color(0xFF68726C),
    sageStrong = Color(0xFF3D6652),
    sage = Color(0xFF8FAE9B),
    sageSoft = Color(0xFFDCE8DF),
    terracottaStrong = Color(0xFFA4513F),
    terracotta = Color(0xFFD98E73),
    divider = Color(0xFFDED8CE),
    isNight = false,
)

private val DarkLinenColors = LinenColors(
    canvas = Color(0xFF12141C),
    surface = Color(0xFF1B1E27),
    surfaceQuiet = Color(0xFF252832),
    ink = Color(0xFFF5F1EA),
    inkMuted = Color(0xFFB9B4AC),
    sageStrong = Color(0xFFA9C8B5),
    sage = Color(0xFF789A87),
    sageSoft = Color(0xFF2A4034),
    terracottaStrong = Color(0xFFE3A08A),
    terracotta = Color(0xFFB97561),
    divider = Color(0xFF383B45),
    isNight = false,
)

private val NightLinenColors = LinenColors(
    canvas = Color.Black,
    surface = Color(0xFF120D09),
    surfaceQuiet = Color(0xFF17100B),
    ink = Color(0xFFFFEDD7),
    inkMuted = Color(0xFFA5917C),
    sageStrong = Color(0xFFC7AB8C),
    sage = Color(0xFF876E58),
    sageSoft = Color(0xFF211812),
    terracottaStrong = Color(0xFFB34D2F),
    terracotta = Color(0xFF8D3E29),
    divider = Color(0xFF3D3026),
    isNight = true,
)

val LocalLinenColors = staticCompositionLocalOf { LightLinenColors }
val LocalThemeVariant = staticCompositionLocalOf { ThemeVariant.LIGHT }

fun linenColorsFor(variant: ThemeVariant): LinenColors = when (variant) {
    ThemeVariant.LIGHT -> LightLinenColors
    ThemeVariant.DARK -> DarkLinenColors
    ThemeVariant.NIGHT -> NightLinenColors
}

fun linenMaterialColorScheme(variant: ThemeVariant) = linenColorsFor(variant).toMaterialScheme()

private fun LinenColors.toMaterialScheme() = if (this === LightLinenColors) {
    lightColorScheme(
        primary = sageStrong,
        onPrimary = Color.White,
        primaryContainer = sageSoft,
        onPrimaryContainer = ink,
        secondary = terracottaStrong,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF3DED5),
        onSecondaryContainer = ink,
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceQuiet,
        onSurfaceVariant = inkMuted,
        outline = divider,
        error = terracottaStrong,
        onError = Color.White,
    )
} else {
    darkColorScheme(
        primary = sageStrong,
        onPrimary = if (isNight) Color(0xFF25140D) else Color(0xFF183A28),
        primaryContainer = sageSoft,
        onPrimaryContainer = ink,
        secondary = terracottaStrong,
        onSecondary = if (isNight) ink else Color(0xFF3B1710),
        secondaryContainer = terracotta,
        onSecondaryContainer = ink,
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceQuiet,
        onSurfaceVariant = inkMuted,
        outline = divider,
        error = terracottaStrong,
        onError = ink,
    )
}

private val LinenTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Medium,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 22.sp,
        lineHeight = 29.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
)

private val LinenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NenelogTheme(
    variant: ThemeVariant,
    content: @Composable () -> Unit,
) {
    val colors = linenColorsFor(variant)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLinenColors provides colors,
        LocalThemeVariant provides variant,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = LinenTypography,
            shapes = LinenShapes,
            content = content,
        )
    }
}

object LinenDimens {
    val screenHorizontal = 20.dp
    val section = 28.dp
    val item = 16.dp
    val touchTarget = 48.dp
    val buttonHeight = 56.dp
}
