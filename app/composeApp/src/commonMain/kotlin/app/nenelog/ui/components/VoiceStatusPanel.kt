package app.nenelog.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.nenelog.resources.*
import app.nenelog.ui.model.VoiceUiPhase
import app.nenelog.ui.model.VoiceUiStatus
import app.nenelog.ui.theme.LocalLinenColors
import kotlin.math.PI
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

@Composable
fun VoiceStatusPanel(
    status: VoiceUiStatus,
    available: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val effectivePhase = if (available) status.phase else VoiceUiPhase.FAILURE
    val title = when (effectivePhase) {
        VoiceUiPhase.WAITING -> stringResource(Res.string.voice_waiting)
        VoiceUiPhase.LISTENING -> stringResource(Res.string.voice_listening)
        VoiceUiPhase.RECOGNIZED -> stringResource(Res.string.voice_recognized)
        VoiceUiPhase.RESPONDING -> stringResource(Res.string.voice_responding)
        VoiceUiPhase.FAILURE -> stringResource(Res.string.voice_failure)
    }
    val description = when {
        !available -> stringResource(Res.string.voice_unavailable)
        effectivePhase == VoiceUiPhase.WAITING -> stringResource(Res.string.voice_waiting_description)
        effectivePhase == VoiceUiPhase.LISTENING -> stringResource(Res.string.voice_listening_description)
        effectivePhase == VoiceUiPhase.RECOGNIZED && !status.transcript.isNullOrBlank() ->
            stringResource(Res.string.voice_recognized_description, status.transcript)
        effectivePhase == VoiceUiPhase.RECOGNIZED -> stringResource(Res.string.voice_recognized)
        effectivePhase == VoiceUiPhase.RESPONDING -> stringResource(Res.string.voice_responding_description)
        else -> stringResource(Res.string.voice_failure_description)
    }
    val accent = when (effectivePhase) {
        VoiceUiPhase.FAILURE -> colors.terracottaStrong
        VoiceUiPhase.RESPONDING -> colors.terracotta
        VoiceUiPhase.WAITING -> colors.inkMuted
        else -> colors.sageStrong
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                stateDescription = "$title. $description"
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LinenIcon(
                type = if (effectivePhase == VoiceUiPhase.FAILURE) LinenIconType.ERROR else LinenIconType.MIC,
                tint = accent,
                modifier = Modifier.size(22.dp),
                size = 22.dp,
            )
            Text(
                text = stringResource(Res.string.voice_status_title),
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (reducedMotion || effectivePhase !in setOf(VoiceUiPhase.LISTENING, VoiceUiPhase.RESPONDING)) {
            StaticVoiceLine(effectivePhase, accent)
        } else {
            BreathingVoiceLine(effectivePhase, accent)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun BreathingVoiceLine(phase: VoiceUiPhase, color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "voice-breath")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "voice-wave",
    )
    VoiceLine(
        phase = phase,
        color = color,
        amplitude = if (phase == VoiceUiPhase.RESPONDING) 5f + progress * 7f else 3f + progress * 5f,
    )
}

@Composable
private fun StaticVoiceLine(phase: VoiceUiPhase, color: androidx.compose.ui.graphics.Color) {
    VoiceLine(
        phase = phase,
        color = color,
        amplitude = if (phase == VoiceUiPhase.RECOGNIZED) 8f else 0f,
    )
}

@Composable
private fun VoiceLine(
    phase: VoiceUiPhase,
    color: androidx.compose.ui.graphics.Color,
    amplitude: Float,
) {
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val middle = size.height / 2f
        val wave = Path().apply {
            moveTo(0f, middle)
            val steps = 48
            for (index in 1..steps) {
                val x = size.width * index / steps
                val envelope = sin(PI * index / steps).toFloat()
                val frequency = if (phase == VoiceUiPhase.RESPONDING) 5f else 3f
                val y = middle + sin(index / steps.toFloat() * PI.toFloat() * frequency) * amplitude * envelope
                lineTo(x, y)
            }
        }
        drawPath(
            path = wave,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (phase == VoiceUiPhase.FAILURE) {
                    PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx()))
                } else {
                    null
                },
            ),
        )
        if (phase == VoiceUiPhase.RECOGNIZED) {
            drawCircle(color, 3.dp.toPx(), Offset(size.width * .5f, middle))
        }
    }
}
