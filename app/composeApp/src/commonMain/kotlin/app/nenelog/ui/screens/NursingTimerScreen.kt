package app.nenelog.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nenelog.data.NursingCommandResult
import app.nenelog.resources.*
import app.nenelog.ui.components.LinenIcon
import app.nenelog.ui.components.LinenIconType
import app.nenelog.ui.components.VoiceStatusPanel
import app.nenelog.ui.model.VoiceUiStatus
import app.nenelog.ui.presentation.timerDuration
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun NursingTimerScreen(
    status: NursingCommandResult,
    voiceStatus: VoiceUiStatus,
    voiceControlAvailable: Boolean,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onStartSide: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val isRunning = status.stateCode == "running"
    val canStop = isRunning || status.stateCode == "paused"
    val statusLabel = when {
        status.stateCode == "paused" -> stringResource(Res.string.status_paused)
        status.currentSideCode == "left" -> stringResource(Res.string.status_left)
        status.currentSideCode == "right" -> stringResource(Res.string.status_right)
        else -> stringResource(Res.string.status_idle)
    }
    val formattedDuration = timerDuration(status.elapsedMillis)
    val backLabel = stringResource(Res.string.back)
    val timerTitle = stringResource(Res.string.nursing_timer)
    val timerSemantics = stringResource(
        Res.string.timer_state_description,
        statusLabel,
        formattedDuration,
    )

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .sizeIn(minWidth = LinenDimens.touchTarget, minHeight = LinenDimens.touchTarget)
                        .semantics { contentDescription = backLabel },
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    border = BorderStroke(1.dp, colors.divider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
                ) {
                    LinenIcon(LinenIconType.BACK, colors.ink, size = 20.dp)
                }
                Text(
                    text = timerTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

        item {
            val fontScale = LocalDensity.current.fontScale
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LinenDimens.screenHorizontal, vertical = 20.dp)
                    .semantics(mergeDescendants = true) { stateDescription = timerSemantics },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.total_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.inkMuted,
                )
                Text(
                    text = formattedDuration,
                    style = (if (fontScale >= 1.6f) {
                        MaterialTheme.typography.headlineLarge
                    } else {
                        MaterialTheme.typography.displayLarge
                    }).copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.ink,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRunning) colors.sageStrong else colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(horizontal = LinenDimens.screenHorizontal),
            ) {
                val stacked = LocalDensity.current.fontScale >= 1.5f || maxWidth < 330.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SideControl(
                            label = stringResource(Res.string.left),
                            action = sideActionLabel(status.currentSideCode == "left", isRunning),
                            selected = status.currentSideCode == "left",
                            isRunning = isRunning,
                            reducedMotion = reducedMotion,
                            circular = false,
                            onClick = { onStartSide("left") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SideControl(
                            label = stringResource(Res.string.right),
                            action = sideActionLabel(status.currentSideCode == "right", isRunning),
                            selected = status.currentSideCode == "right",
                            isRunning = isRunning,
                            reducedMotion = reducedMotion,
                            circular = false,
                            onClick = { onStartSide("right") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SideControl(
                            label = stringResource(Res.string.left),
                            action = sideActionLabel(status.currentSideCode == "left", isRunning),
                            selected = status.currentSideCode == "left",
                            isRunning = isRunning,
                            reducedMotion = reducedMotion,
                            circular = true,
                            onClick = { onStartSide("left") },
                            modifier = Modifier.weight(1f),
                        )
                        SideControl(
                            label = stringResource(Res.string.right),
                            action = sideActionLabel(status.currentSideCode == "right", isRunning),
                            selected = status.currentSideCode == "right",
                            isRunning = isRunning,
                            reducedMotion = reducedMotion,
                            circular = true,
                            onClick = { onStartSide("right") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onStop,
                enabled = canStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LinenDimens.screenHorizontal, vertical = 24.dp)
                    .height(LinenDimens.buttonHeight),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.terracottaStrong,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = colors.surfaceQuiet,
                    disabledContentColor = colors.inkMuted,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.stop_and_save),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        item {
            Text(
                text = stringResource(
                    if (isRunning) Res.string.timer_running_hint else Res.string.timer_idle_hint,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 18.dp),
            )
        }

        item {
            VoiceStatusPanel(
                status = voiceStatus,
                available = voiceControlAvailable,
                reducedMotion = reducedMotion,
                modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal, vertical = 8.dp),
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun sideActionLabel(selected: Boolean, anyRunning: Boolean): String = when {
    selected -> stringResource(Res.string.in_progress)
    anyRunning -> stringResource(Res.string.switch_side)
    else -> stringResource(Res.string.start)
}

@Composable
private fun SideControl(
    label: String,
    action: String,
    selected: Boolean,
    isRunning: Boolean,
    reducedMotion: Boolean,
    circular: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected && isRunning && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "active-side-breath")
        val alpha by transition.animateFloat(
            initialValue = .82f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4_000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "active-side-alpha",
        )
        SideControlButton(label, action, selected, circular, onClick, modifier.alpha(alpha))
    } else {
        SideControlButton(label, action, selected, circular, onClick, modifier)
    }
}

@Composable
private fun SideControlButton(
    label: String,
    action: String,
    selected: Boolean,
    circular: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalLinenColors.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    Button(
        onClick = onClick,
        modifier = modifier
            .then(
                if (circular) Modifier.aspectRatio(1f)
                else Modifier.height(if (largeText) 136.dp else 88.dp),
            )
            .sizeIn(minWidth = LinenDimens.touchTarget, minHeight = LinenDimens.touchTarget)
            .semantics {
                this.selected = selected
                role = Role.Button
                stateDescription = action
            },
        shape = if (circular) CircleShape else MaterialTheme.shapes.large,
        border = when {
            selected && colors.isNight -> BorderStroke(2.dp, colors.terracottaStrong)
            colors.isNight -> BorderStroke(1.dp, colors.divider)
            else -> null
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                selected && colors.isNight -> colors.surfaceQuiet
                selected -> colors.sageStrong
                else -> colors.sageSoft
            },
            contentColor = when {
                selected && colors.isNight -> colors.ink
                selected -> MaterialTheme.colorScheme.onPrimary
                else -> colors.ink
            },
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
