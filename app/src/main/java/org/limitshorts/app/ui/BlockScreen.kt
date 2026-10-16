package org.limitshorts.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R
import org.limitshorts.app.data.BlockReason
import org.limitshorts.app.data.Store

/**
 * 蓋在短影片上的全螢幕畫面。主要動作永遠是「去休息」。
 * LIMIT:時間用完,可以延長(次數有限)。QUIET:休息時間,不能延長。
 */
@Composable
fun BlockScreen(
    reason: BlockReason,
    platform: String,
    extensionsLeft: Int,
    quietFrom: String,
    quietTo: String,
    onRest: () -> Unit,
    onExtend: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Palette.Ink).systemBarsPadding()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(1.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 一圈走完的環,中間是「停一下」
                Canvas(Modifier.size(132.dp)) {
                    val stroke = 6.dp.toPx()
                    val pad = stroke
                    val s = Size(size.width - pad * 2, size.height - pad * 2)
                    drawArc(Palette.Line, 0f, 360f, false, Offset(pad, pad), s, style = Stroke(2.dp.toPx()))
                    drawArc(
                        Palette.Ember, -90f, 360f, false, Offset(pad, pad), s,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val half = 15.dp.toPx()
                    val gap = 9.dp.toPx()
                    listOf(cx - gap, cx + gap).forEach { x ->
                        drawLine(
                            Palette.Ember, Offset(x, cy - half), Offset(x, cy + half),
                            strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round,
                        )
                    }
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    stringResource(if (reason == BlockReason.QUIET) R.string.block_quiet_title else R.string.block_title),
                    style = TextStyle(
                        color = Palette.Text, fontSize = 40.sp, fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (reason == BlockReason.QUIET) {
                        stringResource(R.string.block_quiet_message, quietFrom, quietTo)
                    } else {
                        stringResource(R.string.block_message, platform)
                    },
                    style = TextStyle(color = Palette.Muted, fontSize = 17.sp, lineHeight = 26.sp),
                    textAlign = TextAlign.Center,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PrimaryButton(stringResource(R.string.block_rest), Palette.Lime, height = 68.dp, textSize = 20, onClick = onRest)
                if (reason == BlockReason.LIMIT) {
                    Spacer(Modifier.height(14.dp))
                    val canExtend = extensionsLeft > 0
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .border(1.dp, Palette.Line, RoundedCornerShape(30.dp))
                            .clickable(enabled = canExtend, role = Role.Button, onClick = onExtend),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.block_extend, Store.EXT_MINUTES),
                            style = TextStyle(
                                color = if (canExtend) Palette.Text else Palette.Muted.copy(alpha = 0.5f),
                                fontSize = 17.sp, fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (canExtend) stringResource(R.string.block_extend_left, extensionsLeft)
                        else stringResource(R.string.block_extend_none),
                        style = TextStyle(color = Palette.Muted, fontSize = 14.sp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
