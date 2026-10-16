package org.limitshorts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R
import org.limitshorts.app.data.QuietHours
import org.limitshorts.app.data.Rule
import org.limitshorts.app.data.Scope
import org.limitshorts.app.data.Store

/**
 * 簡易模式首頁:字更大、按鈕更大、只留兩件事 ——「一天最多看多久」和「晚上要不要休息」。
 */
@Composable
fun SimpleHomeScreen(
    rules: List<Rule>,
    store: Store.Snapshot,
    serviceEnabled: Boolean,
    actions: AppActions,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Ink).systemBarsPadding()) {
        if (!serviceEnabled) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                TopBar(onSettings = onOpenSettings)
                StatusRing(active = false, fraction = 0f, totalMinutes = 0)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrimaryButton(
                        stringResource(R.string.cta_enable), Palette.Ember,
                        height = 76.dp, textSize = 22, onClick = actions.openAccessibility,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.cta_hint),
                        style = TextStyle(color = Palette.Muted, fontSize = 17.sp, lineHeight = 26.sp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
            return@BoxWithConstraints
        }

        val supported = rules.filter { it.supported }
        val totalUsed = supported.sumOf { store.platforms[it.id]?.usedSec ?: 0 }
        val totalAllowed = supported.sumOf { store.platforms[it.id]?.allowedSec ?: 0 }
        val fraction = if (totalAllowed > 0) totalUsed.toFloat() / totalAllowed else 0f

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(onSettings = onOpenSettings)
            Spacer(Modifier.height(8.dp))
            Pill(stringResource(R.string.pill_on), Palette.Lime)
            Spacer(Modifier.height(20.dp))
            StatusRing(active = true, fraction = fraction, totalMinutes = totalUsed / 60, size = 280.dp, bigText = true)
            Spacer(Modifier.height(32.dp))

            supported.forEach { rule ->
                val st = store.platforms[rule.id] ?: return@forEach
                BigLimitCard(rule.name, st, { actions.setLimit(rule.id, it) }) { actions.setScope(rule.id, it) }
                Spacer(Modifier.height(16.dp))
            }
            BigQuietCard(store.quiet, actions.setQuietEnabled)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BigLimitCard(
    name: String,
    state: Store.PlatformState,
    onLimit: (Int) -> Unit,
    onScope: (Scope) -> Unit,
) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                modifier = Modifier.weight(1f),
                style = TextStyle(color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.Medium),
            )
            Text(
                stringResource(R.string.used_today, state.usedSec / 60),
                style = TextStyle(color = Palette.Muted, fontSize = 17.sp),
            )
        }
        UsageBar(state)
        Spacer(Modifier.height(20.dp))
        ScopeSwitch(state.scope, onScope, big = true)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.simple_limit_title),
            style = TextStyle(color = Palette.Muted, fontSize = 18.sp),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("−", stringResource(R.string.limit_less), state.limitMin > Store.MIN_LIMIT, size = 72.dp) {
                onLimit(state.limitMin - Store.LIMIT_STEP)
            }
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    state.limitMin.toString(),
                    style = TextStyle(color = Palette.Text, fontSize = 56.sp, fontWeight = FontWeight.Light),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.unit_min),
                    modifier = Modifier.padding(bottom = 10.dp),
                    style = TextStyle(color = Palette.Muted, fontSize = 18.sp),
                )
            }
            RoundButton("+", stringResource(R.string.limit_more), state.limitMin < Store.MAX_LIMIT, size = 72.dp) {
                onLimit(state.limitMin + Store.LIMIT_STEP)
            }
        }
    }
}

@Composable
private fun BigQuietCard(quiet: QuietHours, onToggle: (Boolean) -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.simple_quiet_title),
                    style = TextStyle(color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        R.string.quiet_range, QuietHours.format(quiet.startMin), QuietHours.format(quiet.endMin),
                    ),
                    style = TextStyle(color = Palette.Muted, fontSize = 17.sp),
                )
            }
            PillSwitch(quiet.enabled, onToggle, stringResource(R.string.simple_quiet_title))
        }
    }
}
