package app.nenelog.ui.presentation

import androidx.compose.runtime.Composable
import app.nenelog.domain.BottleKind
import app.nenelog.domain.DiaperAmount
import app.nenelog.domain.Event
import app.nenelog.domain.EventPayload
import app.nenelog.domain.EventSource
import app.nenelog.domain.Side
import app.nenelog.resources.*
import app.nenelog.ui.components.LinenIconType
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
fun durationLabel(millis: Long): String {
    val safeMillis = millis.coerceAtLeast(0)
    val totalSeconds = safeMillis / 1_000
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> stringResource(Res.string.duration_hours_minutes, hours, minutes)
        totalMinutes > 0 -> stringResource(Res.string.duration_minutes, totalMinutes)
        else -> stringResource(Res.string.duration_seconds, totalSeconds)
    }
}

fun timerDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

@Composable
fun relativeTimeLabel(occurredAt: Long, now: Long): String {
    val minutes = ((now - occurredAt).coerceAtLeast(0) / 60_000)
    return when {
        minutes < 1 -> stringResource(Res.string.just_now)
        minutes < 60 -> stringResource(Res.string.minutes_ago, minutes)
        minutes < 24 * 60 -> stringResource(Res.string.hours_ago, minutes / 60)
        else -> stringResource(Res.string.days_ago, minutes / (24 * 60))
    }
}

@Composable
fun Event.typeLabel(): String = stringResource(
    when (payload) {
        is EventPayload.Nursing -> Res.string.event_nursing
        is EventPayload.Bottle -> Res.string.event_bottle
        is EventPayload.Pumping -> Res.string.event_pumping
        is EventPayload.Diaper -> Res.string.event_diaper
        is EventPayload.Sleep -> Res.string.event_sleep
        is EventPayload.Temperature -> Res.string.event_temperature
        is EventPayload.Medicine -> Res.string.event_medicine
        EventPayload.Bath -> Res.string.event_bath
        is EventPayload.Growth -> Res.string.event_growth
        is EventPayload.Memo -> Res.string.event_memo
        is EventPayload.Revocation -> Res.string.event_memo
    },
)

fun Event.iconType(): LinenIconType = when (payload) {
    is EventPayload.Nursing -> LinenIconType.NURSING
    is EventPayload.Bottle -> LinenIconType.BOTTLE
    is EventPayload.Pumping -> LinenIconType.PUMPING
    is EventPayload.Diaper -> LinenIconType.DIAPER
    is EventPayload.Sleep -> LinenIconType.SLEEP
    is EventPayload.Temperature -> LinenIconType.TEMPERATURE
    is EventPayload.Medicine -> LinenIconType.MEDICINE
    EventPayload.Bath -> LinenIconType.BATH
    is EventPayload.Growth -> LinenIconType.GROWTH
    is EventPayload.Memo -> LinenIconType.MEMO
    is EventPayload.Revocation -> LinenIconType.MEMO
}

@Composable
fun Event.detailLabel(): String = when (val value = payload) {
    is EventPayload.Nursing -> {
        val left = value.segments.filter { it.side == Side.LEFT }.sumOf { it.durationMillis }
        val right = value.segments.filter { it.side == Side.RIGHT }.sumOf { it.durationMillis }
        stringResource(Res.string.side_duration, durationLabel(left), durationLabel(right))
    }
    is EventPayload.Bottle -> {
        val kind = when (value.kind) {
            BottleKind.FORMULA -> stringResource(Res.string.bottle_formula)
            BottleKind.BREAST_MILK -> stringResource(Res.string.bottle_breast_milk)
            BottleKind.MIXED -> stringResource(Res.string.bottle_mixed)
        }
        "$kind · ${stringResource(Res.string.volume_ml, value.amountMl)}"
    }
    is EventPayload.Pumping -> buildList {
        add(if (value.side == Side.LEFT) stringResource(Res.string.left) else stringResource(Res.string.right))
        value.amountMl?.let { add(stringResource(Res.string.volume_ml, it)) }
        value.durationSec?.let { add(durationLabel(it * 1_000L)) }
    }.joinToString(" · ")
    is EventPayload.Diaper -> {
        val contents = when {
            value.pee && value.poop -> stringResource(Res.string.diaper_both)
            value.poop -> stringResource(Res.string.diaper_dirty)
            else -> stringResource(Res.string.diaper_wet)
        }
        val amount = when (value.amount) {
            DiaperAmount.S -> stringResource(Res.string.amount_small)
            DiaperAmount.M -> stringResource(Res.string.amount_medium)
            DiaperAmount.L -> stringResource(Res.string.amount_large)
            null -> null
        }
        listOfNotNull(contents, amount).joinToString(" · ")
    }
    is EventPayload.Sleep -> durationLabel(
        ((value.endedAt ?: value.startedAt).epochMillis - value.startedAt.epochMillis).coerceAtLeast(0),
    )
    is EventPayload.Temperature ->
        stringResource(Res.string.temperature_celsius, value.celsius.cleanDecimal())
    is EventPayload.Medicine -> value.name ?: value.note ?: stringResource(Res.string.medicine_unnamed)
    EventPayload.Bath -> stringResource(Res.string.event_bath)
    is EventPayload.Growth -> buildList {
        value.weightG?.let { add(stringResource(Res.string.weight_grams, it)) }
        value.heightMm?.let {
            add(stringResource(Res.string.height_centimeters, (it / 10.0).cleanDecimal()))
        }
    }.joinToString(" · ")
    is EventPayload.Memo -> value.text
    is EventPayload.Revocation -> ""
}

@Composable
fun Event.trailingValue(): String? = when (val value = payload) {
    is EventPayload.Nursing -> durationLabel(value.totalDurationMillis)
    is EventPayload.Bottle -> stringResource(Res.string.volume_ml, value.amountMl)
    is EventPayload.Sleep -> durationLabel(
        ((value.endedAt ?: value.startedAt).epochMillis - value.startedAt.epochMillis).coerceAtLeast(0),
    )
    is EventPayload.Temperature ->
        stringResource(Res.string.temperature_celsius, value.celsius.cleanDecimal())
    else -> null
}

@Composable
fun Event.sourceLabel(): String = stringResource(
    when (source) {
        EventSource.SIRI -> Res.string.source_siri
        EventSource.VOICE_L1, EventSource.VOICE_L2 -> Res.string.source_voice
        EventSource.WIDGET -> Res.string.source_widget
        EventSource.IMPORT -> Res.string.source_import
        EventSource.TAP -> Res.string.source_tap
    },
)

private fun Double.cleanDecimal(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) rounded.toInt().toString() else rounded.toString()
}
