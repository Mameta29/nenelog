package app.nenelog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

enum class LinenIconType {
    HOME,
    JOURNAL,
    SUMMARY,
    SETTINGS,
    MIC,
    NURSING,
    BOTTLE,
    PUMPING,
    DIAPER,
    SLEEP,
    TEMPERATURE,
    MEDICINE,
    BATH,
    MEMO,
    GROWTH,
    BACK,
    CHEVRON_RIGHT,
    CHECK,
    ERROR,
    PLUS,
    TRASH,
}

/** Quiet Linen's single 2dp line-icon family. Labels remain on the parent semantics node. */
@Composable
fun LinenIcon(
    type: LinenIconType,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size).clearAndSetSemantics { }) {
        val side = min(this.size.width, this.size.height)
        val left = (this.size.width - side) / 2f
        val top = (this.size.height - side) / 2f
        fun p(x: Float, y: Float) = Offset(left + side * x, top + side * y)
        val stroke = Stroke(width = maxOf(1.5.dp.toPx(), side * 0.075f), cap = StrokeCap.Round)

        when (type) {
            LinenIconType.HOME -> {
                val roof = Path().apply {
                    moveTo(p(.16f, .47f).x, p(.16f, .47f).y)
                    lineTo(p(.50f, .18f).x, p(.50f, .18f).y)
                    lineTo(p(.84f, .47f).x, p(.84f, .47f).y)
                }
                drawPath(roof, tint, style = stroke)
                drawRoundRect(
                    tint,
                    topLeft = p(.25f, .43f),
                    size = Size(side * .5f, side * .4f),
                    cornerRadius = CornerRadius(side * .06f),
                    style = stroke,
                )
                drawLine(tint, p(.5f, .61f), p(.5f, .83f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.JOURNAL -> {
                drawLine(tint, p(.27f, .18f), p(.27f, .82f), stroke.width, StrokeCap.Round)
                listOf(.28f, .5f, .72f).forEach { y ->
                    drawCircle(tint, side * .045f, p(.27f, y))
                    drawLine(tint, p(.43f, y), p(.82f, y), stroke.width, StrokeCap.Round)
                }
            }
            LinenIconType.SUMMARY -> {
                drawLine(tint, p(.18f, .82f), p(.82f, .82f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.27f, .72f), p(.27f, .53f), stroke.width * 1.5f, StrokeCap.Round)
                drawLine(tint, p(.50f, .72f), p(.50f, .31f), stroke.width * 1.5f, StrokeCap.Round)
                drawLine(tint, p(.73f, .72f), p(.73f, .42f), stroke.width * 1.5f, StrokeCap.Round)
            }
            LinenIconType.SETTINGS -> {
                listOf(.28f, .5f, .72f).forEachIndexed { index, y ->
                    drawLine(tint, p(.16f, y), p(.84f, y), stroke.width, StrokeCap.Round)
                    val x = listOf(.37f, .67f, .45f)[index]
                    drawCircle(tint, side * .075f, p(x, y), style = Stroke(stroke.width))
                }
            }
            LinenIconType.MIC -> {
                drawRoundRect(
                    tint,
                    topLeft = p(.36f, .14f),
                    size = Size(side * .28f, side * .45f),
                    cornerRadius = CornerRadius(side * .14f),
                    style = stroke,
                )
                drawArc(
                    tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = p(.24f, .34f),
                    size = Size(side * .52f, side * .38f),
                    style = stroke,
                )
                drawLine(tint, p(.5f, .72f), p(.5f, .86f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.37f, .86f), p(.63f, .86f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.NURSING -> {
                drawCircle(tint, side * .22f, p(.32f, .52f), style = stroke)
                drawCircle(tint, side * .22f, p(.68f, .52f), style = stroke)
                drawCircle(tint, side * .035f, p(.32f, .52f))
                drawCircle(tint, side * .035f, p(.68f, .52f))
            }
            LinenIconType.BOTTLE -> {
                drawRoundRect(
                    tint,
                    topLeft = p(.34f, .31f),
                    size = Size(side * .32f, side * .52f),
                    cornerRadius = CornerRadius(side * .08f),
                    style = stroke,
                )
                drawLine(tint, p(.4f, .31f), p(.4f, .21f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.6f, .31f), p(.6f, .21f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.4f, .21f), p(.6f, .21f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.42f, .55f), p(.58f, .55f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.42f, .68f), p(.58f, .68f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.PUMPING -> {
                drawCircle(tint, side * .17f, p(.35f, .42f), style = stroke)
                drawLine(tint, p(.51f, .42f), p(.76f, .42f), stroke.width, StrokeCap.Round)
                drawArc(
                    tint,
                    180f,
                    180f,
                    false,
                    topLeft = p(.6f, .42f),
                    size = Size(side * .22f, side * .28f),
                    style = stroke,
                )
                drawLine(tint, p(.71f, .7f), p(.71f, .82f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.DIAPER -> {
                val path = Path().apply {
                    moveTo(p(.18f, .3f).x, p(.18f, .3f).y)
                    quadraticTo(p(.5f, .42f).x, p(.5f, .42f).y, p(.82f, .3f).x, p(.82f, .3f).y)
                    lineTo(p(.73f, .76f).x, p(.73f, .76f).y)
                    quadraticTo(p(.5f, .86f).x, p(.5f, .86f).y, p(.27f, .76f).x, p(.27f, .76f).y)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, p(.23f, .51f), p(.39f, .59f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.77f, .51f), p(.61f, .59f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.SLEEP -> {
                val moon = Path().apply {
                    moveTo(p(.69f, .18f).x, p(.69f, .18f).y)
                    cubicTo(
                        p(.39f, .25f).x, p(.39f, .25f).y,
                        p(.32f, .63f).x, p(.32f, .63f).y,
                        p(.67f, .78f).x, p(.67f, .78f).y,
                    )
                    cubicTo(
                        p(.24f, .9f).x, p(.24f, .9f).y,
                        p(.08f, .34f).x, p(.08f, .34f).y,
                        p(.69f, .18f).x, p(.69f, .18f).y,
                    )
                }
                drawPath(moon, tint, style = stroke)
            }
            LinenIconType.TEMPERATURE -> {
                drawRoundRect(
                    tint,
                    topLeft = p(.42f, .16f),
                    size = Size(side * .16f, side * .52f),
                    cornerRadius = CornerRadius(side * .08f),
                    style = stroke,
                )
                drawCircle(tint, side * .13f, p(.5f, .72f), style = stroke)
                drawLine(tint, p(.5f, .35f), p(.5f, .68f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.MEDICINE -> {
                drawRoundRect(
                    tint,
                    topLeft = p(.2f, .31f),
                    size = Size(side * .6f, side * .38f),
                    cornerRadius = CornerRadius(side * .19f),
                    style = stroke,
                )
                drawLine(tint, p(.5f, .31f), p(.5f, .69f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.27f, .38f), p(.44f, .62f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.BATH -> {
                val drop = Path().apply {
                    moveTo(p(.5f, .14f).x, p(.5f, .14f).y)
                    cubicTo(
                        p(.73f, .42f).x, p(.73f, .42f).y,
                        p(.77f, .53f).x, p(.77f, .53f).y,
                        p(.77f, .64f).x, p(.77f, .64f).y,
                    )
                    cubicTo(
                        p(.77f, .82f).x, p(.77f, .82f).y,
                        p(.23f, .82f).x, p(.23f, .82f).y,
                        p(.23f, .64f).x, p(.23f, .64f).y,
                    )
                    cubicTo(
                        p(.23f, .53f).x, p(.23f, .53f).y,
                        p(.27f, .42f).x, p(.27f, .42f).y,
                        p(.5f, .14f).x, p(.5f, .14f).y,
                    )
                }
                drawPath(drop, tint, style = stroke)
            }
            LinenIconType.MEMO -> {
                drawRoundRect(
                    tint,
                    topLeft = p(.21f, .16f),
                    size = Size(side * .58f, side * .68f),
                    cornerRadius = CornerRadius(side * .07f),
                    style = stroke,
                )
                listOf(.37f, .51f, .65f).forEach { y ->
                    drawLine(tint, p(.33f, y), p(.67f, y), stroke.width, StrokeCap.Round)
                }
            }
            LinenIconType.GROWTH -> {
                drawLine(tint, p(.25f, .78f), p(.75f, .22f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.25f, .78f), p(.25f, .53f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.25f, .78f), p(.5f, .78f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.6f, .22f), p(.75f, .22f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.75f, .22f), p(.75f, .37f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.BACK -> {
                drawLine(tint, p(.68f, .18f), p(.32f, .5f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.32f, .5f), p(.68f, .82f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.CHEVRON_RIGHT -> {
                drawLine(tint, p(.36f, .2f), p(.66f, .5f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.66f, .5f), p(.36f, .8f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.CHECK -> {
                drawLine(tint, p(.18f, .53f), p(.42f, .75f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.42f, .75f), p(.84f, .25f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.ERROR -> {
                drawCircle(tint, side * .34f, p(.5f, .5f), style = stroke)
                drawLine(tint, p(.5f, .28f), p(.5f, .55f), stroke.width, StrokeCap.Round)
                drawCircle(tint, side * .045f, p(.5f, .7f))
            }
            LinenIconType.PLUS -> {
                drawLine(tint, p(.2f, .5f), p(.8f, .5f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.5f, .2f), p(.5f, .8f), stroke.width, StrokeCap.Round)
            }
            LinenIconType.TRASH -> {
                drawRoundRect(
                    tint,
                    topLeft = p(.3f, .3f),
                    size = Size(side * .4f, side * .5f),
                    cornerRadius = CornerRadius(side * .04f),
                    style = stroke,
                )
                drawLine(tint, p(.22f, .3f), p(.78f, .3f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.4f, .2f), p(.6f, .2f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.43f, .43f), p(.43f, .67f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(.57f, .43f), p(.57f, .67f), stroke.width, StrokeCap.Round)
            }
        }
    }
}
