package app.nenelog.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nenelog.domain.Event
import app.nenelog.domain.EventPayload
import app.nenelog.resources.*
import app.nenelog.ui.components.EmptyState
import app.nenelog.ui.components.LinenIcon
import app.nenelog.ui.components.LinenIconType
import app.nenelog.ui.components.ScreenHeader
import app.nenelog.ui.platform.formatLocalDateHeading
import app.nenelog.ui.platform.formatLocalTime
import app.nenelog.ui.platform.localDateKey
import app.nenelog.ui.presentation.detailLabel
import app.nenelog.ui.presentation.iconType
import app.nenelog.ui.presentation.sourceLabel
import app.nenelog.ui.presentation.trailingValue
import app.nenelog.ui.presentation.typeLabel
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalScreen(
    events: List<Event>,
    onEventSelected: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupedEvents = events.groupBy { localDateKey(it.occurredAt.epochMillis) }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            ScreenHeader(
                title = stringResource(Res.string.journal_title),
                subtitle = stringResource(Res.string.journal_subtitle),
                modifier = Modifier.padding(
                    start = LinenDimens.screenHorizontal,
                    end = LinenDimens.screenHorizontal,
                    top = 24.dp,
                    bottom = 18.dp,
                ),
            )
        }

        if (events.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(Res.string.timeline_empty_title),
                    body = stringResource(Res.string.timeline_empty),
                    icon = LinenIconType.JOURNAL,
                    modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal),
                )
            }
        } else {
            groupedEvents.values.forEach { dayEvents ->
                val dayEpoch = dayEvents.first().occurredAt.epochMillis
                stickyHeader(key = "date-${localDateKey(dayEpoch)}") {
                    JournalDateHeader(dayEpoch)
                }
                items(dayEvents, key = { it.id }) { event ->
                    JournalEventRow(
                        event = event,
                        emphasized = event.id == events.firstOrNull()?.id,
                        onClick = { onEventSelected(event) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
fun JournalDateHeader(
    epochMillis: Long,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .padding(start = LinenDimens.screenHorizontal, end = LinenDimens.screenHorizontal, top = 16.dp),
    ) {
        Text(
            text = formatLocalDateHeading(epochMillis),
            style = MaterialTheme.typography.labelLarge,
            color = colors.ink,
            modifier = Modifier.padding(bottom = 10.dp).semantics { heading() },
        )
        HorizontalDivider(color = colors.divider)
    }
}

@Composable
fun JournalEventRow(
    event: Event,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val timeColumnWidth = if (largeText) 112.dp else 62.dp
    val title = event.typeLabel()
    val details = event.detailLabel()
    val source = event.sourceLabel()
    val time = formatLocalTime(event.occurredAt.epochMillis)
    val trailing = event.trailingValue()
    val autoClosed = (event.payload as? EventPayload.Nursing)?.autoClosed == true
    val supportText = if (autoClosed) "$source · ${stringResource(Res.string.auto_closed)}" else source
    val spokenDescription = stringResource(
        Res.string.timeline_row_description,
        title,
        time,
        details,
        supportText,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = spokenDescription
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 80.dp)
                .padding(horizontal = LinenDimens.screenHorizontal, vertical = 13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = time,
                style = (if (largeText) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                }).copy(
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.inkMuted,
                modifier = Modifier.width(timeColumnWidth).padding(top = 2.dp),
            )
            if (emphasized) {
                Box(
                    Modifier
                        .padding(end = 10.dp)
                        .width(3.dp)
                        .height(48.dp)
                        .background(colors.sageStrong, MaterialTheme.shapes.extraSmall),
                )
            } else {
                Spacer(Modifier.width(13.dp))
            }
            LinenIcon(
                type = event.iconType(),
                tint = colors.sageStrong,
                modifier = Modifier.padding(top = 1.dp).size(22.dp),
                size = 22.dp,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                    )
                    if (trailing != null) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = trailing,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.ink,
                        )
                    }
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = supportText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (autoClosed) colors.terracottaStrong else colors.inkMuted,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(
                start = LinenDimens.screenHorizontal + timeColumnWidth,
                end = LinenDimens.screenHorizontal,
            ),
            color = colors.divider,
        )
    }
}
