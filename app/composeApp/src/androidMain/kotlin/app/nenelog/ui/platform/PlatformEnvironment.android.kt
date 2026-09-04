package app.nenelog.ui.platform

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal actual fun formatLocalTime(epochMillis: Long, localeTag: String): String =
    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag(localeTag))
        .format(Date(epochMillis))

internal actual fun formatLocalDateHeading(epochMillis: Long, localeTag: String): String =
    DateFormat.getDateInstance(DateFormat.FULL, Locale.forLanguageTag(localeTag))
        .format(Date(epochMillis))

internal actual fun localDateKey(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))

internal actual fun localHour(epochMillis: Long): Int = Calendar.getInstance().run {
    timeInMillis = epochMillis
    get(Calendar.HOUR_OF_DAY)
}

internal actual fun startOfLocalDay(epochMillis: Long): Long = Calendar.getInstance().run {
    timeInMillis = epochMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    timeInMillis
}

internal actual fun startOfRollingWeek(epochMillis: Long): Long = Calendar.getInstance().run {
    timeInMillis = startOfLocalDay(epochMillis)
    add(Calendar.DAY_OF_MONTH, -6)
    timeInMillis
}

@Composable
internal actual fun platformReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}
