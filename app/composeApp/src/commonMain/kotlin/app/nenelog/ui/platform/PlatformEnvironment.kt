package app.nenelog.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale

@Composable
internal fun formatLocalTime(epochMillis: Long): String =
    formatLocalTime(epochMillis, Locale.current.toLanguageTag())

@Composable
internal fun formatLocalDateHeading(epochMillis: Long): String =
    formatLocalDateHeading(epochMillis, Locale.current.toLanguageTag())

internal expect fun formatLocalTime(epochMillis: Long, localeTag: String): String
internal expect fun formatLocalDateHeading(epochMillis: Long, localeTag: String): String
internal expect fun localDateKey(epochMillis: Long): String
internal expect fun localHour(epochMillis: Long): Int
internal expect fun startOfLocalDay(epochMillis: Long): Long
internal expect fun startOfRollingWeek(epochMillis: Long): Long

@Composable
internal expect fun platformReducedMotionEnabled(): Boolean
