package app.nenelog.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nenelog.data.CareSummary
import app.nenelog.domain.Event
import app.nenelog.domain.EventPayload
import app.nenelog.resources.*
import app.nenelog.ui.components.EmptyState
import app.nenelog.ui.components.LinenIconType
import app.nenelog.ui.components.ScreenHeader
import app.nenelog.ui.model.SummaryRange
import app.nenelog.ui.presentation.durationLabel
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun SummaryScreen(
    selectedRange: SummaryRange,
    summary: CareSummary,
    events: List<Event>,
    rangeStartEpochMillis: Long,
    rangeEndEpochMillis: Long,
    onRangeSelected: (SummaryRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val relevantEvents = events.filter {
        it.occurredAt.epochMillis in rangeStartEpochMillis until rangeEndEpochMillis
    }
    val feedingTimes = relevantEvents
        .filter { it.payload is EventPayload.Nursing || it.payload is EventPayload.Bottle }
        .map { it.occurredAt.epochMillis }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            ScreenHeader(
                title = stringResource(Res.string.summary_title),
                subtitle = stringResource(Res.string.summary_subtitle),
                modifier = Modifier.padding(
                    start = LinenDimens.screenHorizontal,
                    end = LinenDimens.screenHorizontal,
                    top = 24.dp,
                    bottom = 20.dp,
                ),
            )
        }
        item {
            SummaryRangeControl(
                selectedRange = selectedRange,
                onRangeSelected = onRangeSelected,
                modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal),
            )
        }
        if (summary.isEmpty) {
            item {
                EmptyState(
                    title = stringResource(Res.string.summary_empty_title),
                    body = stringResource(Res.string.summary_empty_body),
                    icon = LinenIconType.SUMMARY,
                    modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal),
                )
            }
        } else {
            item {
                SummaryMetrics(
                    summary = summary,
                    modifier = Modifier.padding(
                        start = LinenDimens.screenHorizontal,
                        end = LinenDimens.screenHorizontal,
                        top = 28.dp,
                    ),
                )
            }
            item {
                FeedingRhythm(
                    feedingTimes = feedingTimes,
                    rangeStartEpochMillis = rangeStartEpochMillis,
                    rangeEndEpochMillis = rangeEndEpochMillis,
                    modifier = Modifier.padding(
                        start = LinenDimens.screenHorizontal,
                        end = LinenDimens.screenHorizontal,
                        top = 32.dp,
                    ),
                )
            }
            item {
                val colors = LocalLinenColors.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LinenDimens.screenHorizontal, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.sleep_total),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.inkMuted,
                    )
                    Text(
                        text = durationLabel(summary.sleepDurationMillis),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.ink,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun SummaryRangeControl(
    selectedRange: SummaryRange,
    onRangeSelected: (SummaryRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceQuiet,
    ) {
        Row(Modifier.padding(4.dp)) {
            listOf(
                SummaryRange.TODAY to stringResource(Res.string.summary_today),
                SummaryRange.SEVEN_DAYS to stringResource(Res.string.summary_seven_days),
            ).forEach { (range, label) ->
                val selected = range == selectedRange
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = LinenDimens.touchTarget)
                        .clickable(role = Role.Tab) { onRangeSelected(range) }
                        .semantics {
                            this.selected = selected
                            role = Role.Tab
                            contentDescription = label
                        },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) colors.surface else colors.surfaceQuiet,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.sageStrong else colors.inkMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetrics(summary: CareSummary, modifier: Modifier = Modifier) {
    val metrics = listOf(
        stringResource(Res.string.feeding_count) to
            stringResource(Res.string.count_value, summary.feedingCount),
        stringResource(Res.string.nursing_time) to durationLabel(summary.nursingDurationMillis),
        stringResource(Res.string.bottle_total) to
            stringResource(Res.string.volume_ml, summary.bottleAmountMl),
        stringResource(Res.string.diaper_count) to
            stringResource(Res.string.count_value, summary.diaperCount),
    )
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stack = LocalDensity.current.fontScale >= 1.6f || maxWidth < 320.dp
        if (stack) {
            Column {
                metrics.forEachIndexed { index, metric ->
                    MetricCell(metric.first, metric.second, Modifier.fillMaxWidth())
                    if (index != metrics.lastIndex) HorizontalDivider(color = LocalLinenColors.current.divider)
                }
            }
        } else {
            Column {
                Row {
                    MetricCell(metrics[0].first, metrics[0].second, Modifier.weight(1f))
                    MetricCell(metrics[1].first, metrics[1].second, Modifier.weight(1f))
                }
                HorizontalDivider(color = LocalLinenColors.current.divider)
                Row {
                    MetricCell(metrics[2].first, metrics[2].second, Modifier.weight(1f))
                    MetricCell(metrics[3].first, metrics[3].second, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalLinenColors.current
    Column(modifier = modifier.padding(vertical = 18.dp, horizontal = 12.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontFeatureSettings = "tnum",
            ),
            color = colors.ink,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun FeedingRhythm(
    feedingTimes: List<Long>,
    rangeStartEpochMillis: Long,
    rangeEndEpochMillis: Long,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val description = stringResource(Res.string.feeding_rhythm_description, feedingTimes.size)
    Column(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Text(
            text = stringResource(Res.string.feeding_rhythm),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = 3.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(vertical = 20.dp),
        ) {
            val middle = size.height / 2f
            drawLine(
                color = colors.divider,
                start = Offset(0f, middle),
                end = Offset(size.width, middle),
                strokeWidth = 1.dp.toPx(),
            )
            val duration = (rangeEndEpochMillis - rangeStartEpochMillis).coerceAtLeast(1L)
            feedingTimes.forEachIndexed { index, epochMillis ->
                val ratio = ((epochMillis - rangeStartEpochMillis).toDouble() / duration)
                    .coerceIn(0.0, 1.0)
                    .toFloat()
                val x = size.width * ratio
                val height = if (index % 2 == 0) size.height * .65f else size.height * .45f
                drawLine(
                    color = if (index % 2 == 0) colors.sageStrong else colors.terracottaStrong,
                    start = Offset(x, middle - height / 2f),
                    end = Offset(x, middle + height / 2f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
