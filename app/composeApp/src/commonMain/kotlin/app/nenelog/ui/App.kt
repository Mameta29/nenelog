package app.nenelog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.nenelog.data.NursingCommandResult
import app.nenelog.data.NursingSessionService
import app.nenelog.data.NursingTimelineItem
import app.nenelog.resources.Res
import app.nenelog.resources.app_name
import app.nenelog.resources.auto_closed
import app.nenelog.resources.duration_minutes
import app.nenelog.resources.hours_ago
import app.nenelog.resources.in_progress
import app.nenelog.resources.just_now
import app.nenelog.resources.left
import app.nenelog.resources.minutes_ago
import app.nenelog.resources.nursing_record
import app.nenelog.resources.nursing_timer
import app.nenelog.resources.right
import app.nenelog.resources.side_duration
import app.nenelog.resources.source_siri
import app.nenelog.resources.source_tap
import app.nenelog.resources.source_voice
import app.nenelog.resources.start
import app.nenelog.resources.status_idle
import app.nenelog.resources.status_left
import app.nenelog.resources.status_paused
import app.nenelog.resources.status_right
import app.nenelog.resources.stop_and_save
import app.nenelog.resources.switch_side
import app.nenelog.resources.tagline
import app.nenelog.resources.timeline_empty
import app.nenelog.resources.timeline_title
import app.nenelog.resources.voice_listening
import app.nenelog.resources.voice_unavailable
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private val LightColors = lightColorScheme(
    primary = Color(0xFF355F49),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E9DB),
    onPrimaryContainer = Color(0xFF153624),
    secondary = Color(0xFFA6533D),
    onSecondary = Color.White,
    background = Color(0xFFFAF6F0),
    onBackground = Color(0xFF24231F),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF24231F),
    surfaceVariant = Color(0xFFEDE7DE),
    onSurfaceVariant = Color(0xFF554F47),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8D2C1),
    onPrimary = Color(0xFF183A28),
    primaryContainer = Color(0xFF294C39),
    onPrimaryContainer = Color(0xFFD7E9DB),
    secondary = Color(0xFFF0B09A),
    onSecondary = Color(0xFF542114),
    background = Color(0xFF12141C),
    onBackground = Color(0xFFF0ECE5),
    surface = Color(0xFF1B1E28),
    onSurface = Color(0xFFF0ECE5),
    surfaceVariant = Color(0xFF292C36),
    onSurfaceVariant = Color(0xFFD0CBC3),
)

@Composable
fun App(
    service: NursingSessionService,
    voiceControlAvailable: Boolean = false,
    voiceListening: Boolean = false,
    onVoiceSessionStart: () -> Unit = {},
    onVoiceSessionStop: () -> Unit = {},
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        NursingHome(
            service = service,
            voiceControlAvailable = voiceControlAvailable,
            voiceListening = voiceListening,
            onVoiceSessionStart = onVoiceSessionStart,
            onVoiceSessionStop = onVoiceSessionStop,
        )
    }
}

@Composable
private fun NursingHome(
    service: NursingSessionService,
    voiceControlAvailable: Boolean,
    voiceListening: Boolean,
    onVoiceSessionStart: () -> Unit,
    onVoiceSessionStop: () -> Unit,
) {
    var status by remember(service) { mutableStateOf(service.status(nowEpochMillis())) }
    var timeline by remember(service) { mutableStateOf(service.timeline()) }

    fun refresh() {
        status = service.status(nowEpochMillis())
        timeline = service.timeline()
        if (status.stateCode == "running") {
            onVoiceSessionStart()
        } else {
            onVoiceSessionStop()
        }
    }

    LifecycleResumeEffect(service, voiceControlAvailable, voiceListening) {
        refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(status.stateCode, status.currentSideCode, voiceListening) {
        while (status.stateCode == "running") {
            delay(1_000)
            val updatedStatus = service.status(nowEpochMillis())
            status = updatedStatus
            if (updatedStatus.stateCode != "running") {
                timeline = service.timeline()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            item {
                TimerCard(
                    status = status,
                    voiceControlAvailable = voiceControlAvailable,
                    voiceListening = voiceListening,
                    onStart = { side ->
                        val wasRunning = status.stateCode == "running"
                        status = service.start(
                            sideCode = side,
                            epochMillis = nowEpochMillis(),
                            sourceCode = NursingSessionService.SOURCE_TAP,
                        )
                        timeline = service.timeline()
                        if (!wasRunning) onVoiceSessionStart()
                    },
                    onStop = {
                        service.stop(nowEpochMillis())
                        onVoiceSessionStop()
                        refresh()
                    },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                Text(
                    text = stringResource(Res.string.timeline_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
                )
            }

            if (timeline.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.timeline_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            } else {
                items(timeline, key = { it.id }) { item ->
                    TimelineCard(
                        item = item,
                        nowEpochMillis = nowEpochMillis(),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun TimerCard(
    status: NursingCommandResult,
    voiceControlAvailable: Boolean,
    voiceListening: Boolean,
    onStart: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSide = status.currentSideCode
    val isRunning = status.stateCode == "running"
    val statusText = when {
        status.stateCode == "paused" -> stringResource(Res.string.status_paused)
        activeSide == "left" -> stringResource(Res.string.status_left)
        activeSide == "right" -> stringResource(Res.string.status_right)
        else -> stringResource(Res.string.status_idle)
    }

    Card(
        modifier = modifier.fillMaxWidth().semantics {
            stateDescription = statusText
        },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.nursing_timer),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(status.elapsedMillis),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = if (isRunning) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp),
            )

            if (isRunning && voiceControlAvailable) {
                Text(
                    text = stringResource(
                        if (voiceListening) Res.string.voice_listening
                        else Res.string.voice_unavailable,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (voiceListening) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SideButton(
                    label = stringResource(Res.string.left),
                    actionLabel = sideActionLabel(activeSide == "left", isRunning),
                    selected = activeSide == "left",
                    onClick = { onStart("left") },
                    modifier = Modifier.weight(1f),
                )
                SideButton(
                    label = stringResource(Res.string.right),
                    actionLabel = sideActionLabel(activeSide == "right", isRunning),
                    selected = activeSide == "right",
                    onClick = { onStart("right") },
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onStop,
                enabled = isRunning || status.stateCode == "paused",
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Text(stringResource(Res.string.stop_and_save), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun sideActionLabel(selected: Boolean, anyRunning: Boolean): String = when {
    selected -> stringResource(Res.string.in_progress)
    anyRunning -> stringResource(Res.string.switch_side)
    else -> stringResource(Res.string.start)
}

@Composable
private fun SideButton(
    label: String,
    actionLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .semantics {
                this.selected = selected
                role = Role.Button
            },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(actionLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TimelineCard(
    item: NursingTimelineItem,
    nowEpochMillis: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.nursing_record),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (item.autoClosed) {
                        Text(
                            text = stringResource(Res.string.auto_closed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(
                        Res.string.side_duration,
                        item.leftDurationMillis.roundedMinutes(),
                        item.rightDurationMillis.roundedMinutes(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = "${relativeTime(item.occurredAtEpochMillis, nowEpochMillis)} · ${sourceLabel(item.sourceCode)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Text(
                text = stringResource(Res.string.duration_minutes, item.totalDurationMillis.roundedMinutes()),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun relativeTime(occurredAt: Long, now: Long): String {
    val minutes = ((now - occurredAt).coerceAtLeast(0) / 60_000)
    return when {
        minutes < 1 -> stringResource(Res.string.just_now)
        minutes < 60 -> stringResource(Res.string.minutes_ago, minutes)
        else -> stringResource(Res.string.hours_ago, minutes / 60)
    }
}

@Composable
private fun sourceLabel(sourceCode: String): String = when (sourceCode) {
    "siri" -> stringResource(Res.string.source_siri)
    "voice_l1", "voice_l2" -> stringResource(Res.string.source_voice)
    else -> stringResource(Res.string.source_tap)
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
}

private fun Long.roundedMinutes(): Long = when {
    this <= 0 -> 0
    this < 60_000 -> 1
    else -> (this + 30_000) / 60_000
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun nowEpochMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
