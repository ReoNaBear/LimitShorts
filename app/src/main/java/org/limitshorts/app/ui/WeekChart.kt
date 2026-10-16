package org.limitshorts.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R
import org.limitshorts.app.data.Store
import java.time.format.TextStyle as DayStyle

/** 最近 7 天的柱狀圖,最右邊是今天。每一根柱子都有給讀屏軟體的文字描述。 */
@Composable
internal fun WeekChart(days: List<Store.DayTotal>) {
    if (days.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    // 縮放以「至少 30 分鐘」為底,避免一天只看一點點時柱子頂到天花板
    val maxSec = maxOf(days.maxOf { it.seconds }, 30 * 60)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            val isToday = index == days.lastIndex
            val label = day.date.dayOfWeek.getDisplayName(DayStyle.NARROW, locale)
            val minutes = day.seconds / 60
            val description = stringResource(R.string.week_day_desc, day.date.dayOfWeek.getDisplayName(DayStyle.FULL, locale), minutes)
            val ratio by animateFloatAsState(day.seconds.toFloat() / maxSec, tween(600), label = "bar")

            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(96.dp).width(18.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(maxOf(4f, 96f * ratio).dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isToday) Palette.Lime else Palette.Muted.copy(alpha = 0.35f)),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    label,
                    style = TextStyle(
                        color = if (isToday) Palette.Text else Palette.Muted,
                        fontSize = 13.sp,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
