package org.limitshorts.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R

/**
 * 首頁的大圓環。
 * 關閉時:橘色、慢慢轉,中間寫「還沒打開」。
 * 開啟時:弧的長度 = 今天已用 / 今天可用,超過 8 成變橘色;中間是今天看了幾分鐘。
 */
@Composable
internal fun StatusRing(
    active: Boolean,
    fraction: Float,
    totalMinutes: Int,
    size: Dp = 260.dp,
    bigText: Boolean = false,
) {
    val warn = active && fraction >= 0.8f
    val accent by animateColorAsState(
        if (active && !warn) Palette.Lime else Palette.Ember, tween(600), label = "accent",
    )
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(if (active) 18000 else 48000, easing = LinearEasing)),
        label = "angle",
    )
    val breathe by spin.animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "breathe",
    )
    val target = if (active) maxOf(24f, 330f * fraction.coerceIn(0f, 1f)) else 70f
    val sweep by animateFloatAsState(target, tween(900), label = "sweep")

    val caption = stringResource(R.string.ring_caption)
    val offTitle = stringResource(R.string.ring_off_title)
    val offSub = stringResource(R.string.ring_off_sub)
    val unit = stringResource(R.string.unit_min)
    val description = if (active) "$totalMinutes $unit. $caption" else "$offTitle. $offSub"

    Box(
        Modifier.size(size).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke * 2
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.16f * breathe), Color.Transparent),
                    radius = this.size.minDimension / 2f,
                ),
            )
            drawArc(Palette.Line, 0f, 360f, false, topLeft, arcSize, style = Stroke(2.dp.toPx()))
            rotate(angle) {
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, accent.copy(alpha = 0.35f), accent)),
                    startAngle = 0f, sweepAngle = sweep, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (active) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        totalMinutes.toString(),
                        style = TextStyle(
                            color = Palette.Text, fontSize = if (bigText) 76.sp else 64.sp,
                            fontWeight = FontWeight.Light,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        unit,
                        modifier = Modifier.padding(bottom = 12.dp),
                        style = TextStyle(color = Palette.Muted, fontSize = if (bigText) 20.sp else 16.sp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    caption,
                    modifier = Modifier.width(170.dp),
                    style = TextStyle(
                        color = Palette.Muted, fontSize = if (bigText) 16.sp else 13.sp,
                        lineHeight = if (bigText) 23.sp else 19.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    offTitle,
                    style = TextStyle(
                        color = Palette.Text, fontSize = 34.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    offSub,
                    modifier = Modifier.width(170.dp),
                    style = TextStyle(color = Palette.Muted, fontSize = 13.sp, lineHeight = 19.sp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
