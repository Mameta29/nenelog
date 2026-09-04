package app.nenelog.ui.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

private const val DAY_MILLIS = 24L * 60 * 60 * 1_000
private const val UNIX_TO_APPLE_REFERENCE_SECONDS = 978_307_200.0

private fun date(epochMillis: Long): NSDate =
    NSDate(
        timeIntervalSinceReferenceDate =
            epochMillis.toDouble() / 1_000.0 - UNIX_TO_APPLE_REFERENCE_SECONDS,
    )

internal actual fun formatLocalTime(epochMillis: Long, localeTag: String): String = NSDateFormatter().run {
    locale = NSLocale(localeTag)
    setLocalizedDateFormatFromTemplate("jm")
    stringFromDate(date(epochMillis))
}

internal actual fun formatLocalDateHeading(epochMillis: Long, localeTag: String): String = NSDateFormatter().run {
    locale = NSLocale(localeTag)
    setLocalizedDateFormatFromTemplate("EEEE MMMMd")
    stringFromDate(date(epochMillis))
}

internal actual fun localDateKey(epochMillis: Long): String = NSDateFormatter().run {
    locale = NSLocale("en_US_POSIX")
    dateFormat = "yyyy-MM-dd"
    stringFromDate(date(epochMillis))
}

internal actual fun localHour(epochMillis: Long): Int = NSDateFormatter().run {
    locale = NSLocale("en_US_POSIX")
    dateFormat = "H"
    stringFromDate(date(epochMillis)).toIntOrNull() ?: 12
}

internal actual fun startOfLocalDay(epochMillis: Long): Long = NSDateFormatter().run {
    locale = NSLocale("en_US_POSIX")
    dateFormat = "yyyy-MM-dd"
    val day = stringFromDate(date(epochMillis))
    dateFromString(day)?.timeIntervalSinceReferenceDate
        ?.plus(UNIX_TO_APPLE_REFERENCE_SECONDS)
        ?.times(1_000)
        ?.toLong()
        ?: epochMillis
}

internal actual fun startOfRollingWeek(epochMillis: Long): Long =
    startOfLocalDay(epochMillis - 6 * DAY_MILLIS)

@Composable
internal actual fun platformReducedMotionEnabled(): Boolean =
    UIAccessibilityIsReduceMotionEnabled()
