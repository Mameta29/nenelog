package app.nenelog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nenelog.data.NursingCommandResult
import app.nenelog.domain.Event
import app.nenelog.domain.EventPayload
import app.nenelog.resources.*
import app.nenelog.ui.components.EmptyState
import app.nenelog.ui.components.LinenIcon
import app.nenelog.ui.components.LinenIconType
import app.nenelog.ui.components.ScreenHeader
import app.nenelog.ui.components.SectionHeader
import app.nenelog.ui.components.VoiceStatusPanel
import app.nenelog.ui.model.VoiceUiStatus
import app.nenelog.ui.platform.formatLocalDateHeading
import app.nenelog.ui.presentation.durationLabel
import app.nenelog.ui.presentation.timerDuration
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    nowEpochMillis: Long,
    nursingStatus: NursingCommandResult,
    events: List<Event>,
    voiceStatus: VoiceUiStatus,
    voiceControlAvailable: Boolean,
    reducedMotion: Boolean,
    onOpenTimer: () -> Unit,
    onOpenJournal: () -> Unit,
    onEventSelected: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestNursing = events.firstOrNull { it.payload is EventPayload.Nursing }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier.padding(
                    start = LinenDimens.screenHorizontal,
                    end = LinenDimens.screenHorizontal,
                    top = 20.dp,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalLinenColors.current.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = formatLocalDateHeading(nowEpochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalLinenColors.current.inkMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        item {
            NowCard(
                nowEpochMillis = nowEpochMillis,
                nursingStatus = nursingStatus,
                latestNursing = latestNursing,
                onOpenTimer = onOpenTimer,
                modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal, vertical = 22.dp),
            )
        }
        item {
            VoiceStatusPanel(
                status = voiceStatus,
                available = voiceControlAvailable,
                reducedMotion = reducedMotion,
                modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal),
            )
        }
        item {
            SectionHeader(
                title = stringResource(Res.string.recent_records),
                actionLabel = if (events.isEmpty()) null else stringResource(Res.string.see_all),
                onAction = if (events.isEmpty()) null else onOpenJournal,
                modifier = Modifier.padding(
                    start = LinenDimens.screenHorizontal,
                    end = LinenDimens.screenHorizontal,
                    top = 30.dp,
                    bottom = 6.dp,
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
            events.take(3).forEachIndexed { index, event ->
                item(key = event.id) {
                    JournalEventRow(
                        event = event,
                        emphasized = index == 0,
                        onClick = { onEventSelected(event) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun NowCard(
    nowEpochMillis: Long,
    nursingStatus: NursingCommandResult,
    latestNursing: Event?,
    onOpenTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val isRunning = nursingStatus.stateCode == "running" || nursingStatus.stateCode == "paused"
    val endedAt = (latestNursing?.payload as? EventPayload.Nursing)
        ?.segments
        ?.maxOfOrNull { it.endedAt.epochMillis }
    val headline = if (isRunning) {
        timerDuration(nursingStatus.elapsedMillis)
    } else if (endedAt != null) {
        durationLabel((nowEpochMillis - endedAt).coerceAtLeast(0))
    } else {
        null
    }
    val label = when {
        isRunning -> stringResource(Res.string.feeding_in_progress)
        endedAt != null -> stringResource(Res.string.since_last_feed)
        else -> stringResource(Res.string.no_feed_yet)
    }
    val sideStatus = when (nursingStatus.currentSideCode) {
        "left" -> stringResource(Res.string.status_left)
        "right" -> stringResource(Res.string.status_right)
        else -> null
    }
    val semanticsText = listOfNotNull(label, headline, sideStatus).joinToString(". ")
    val openTimerLabel = stringResource(Res.string.open_timer)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 176.dp)
            .clickable(role = Role.Button, onClick = onOpenTimer)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$semanticsText. $openTimerLabel"
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isNight) 0.dp else 1.dp),
        border = if (colors.isNight) BorderStroke(1.dp, colors.divider) else null,
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinenIcon(
                    LinenIconType.NURSING,
                    tint = if (isRunning) colors.terracottaStrong else colors.sageStrong,
                    size = 24.dp,
                )
                Text(
                    text = stringResource(Res.string.home_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
            if (headline != null) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.ink,
                    modifier = Modifier.padding(top = 17.dp),
                )
            }
            Text(
                text = label,
                style = if (headline == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                color = if (isRunning) colors.terracottaStrong else colors.inkMuted,
                modifier = Modifier.padding(top = if (headline == null) 18.dp else 2.dp),
            )
            if (sideStatus != null) {
                Text(
                    text = sideStatus,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.sageStrong,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
