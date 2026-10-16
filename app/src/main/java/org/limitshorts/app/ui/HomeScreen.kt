package org.limitshorts.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R
import org.limitshorts.app.data.Rule
import org.limitshorts.app.data.Scope
import org.limitshorts.app.data.Store

/** 標準模式首頁:圓環、各平台時間、這一週、黑名單。 */
@Composable
fun HomeScreen(
    rules: List<Rule>,
    store: Store.Snapshot,
    serviceEnabled: Boolean,
    actions: AppActions,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Ink).systemBarsPadding()) {
        if (!serviceEnabled) {
            // 還沒打開:上 / 中 / 下三段
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
                    EnableCard(actions.openAccessibility)
                    Spacer(Modifier.height(28.dp))
                    Privacy()
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else {
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
                Spacer(Modifier.height(24.dp))
                StatusRing(active = true, fraction = fraction, totalMinutes = totalUsed / 60)
                Spacer(Modifier.height(36.dp))

                SectionTitle(stringResource(R.string.section_platforms))
                Spacer(Modifier.height(12.dp))
                rules.forEach { rule ->
                    PlatformCard(rule, store.platforms[rule.id], { actions.setLimit(rule.id, it) }) { actions.setScope(rule.id, it) }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.week_title))
                Spacer(Modifier.height(12.dp))
                Card { WeekChart(store.week) }

                Spacer(Modifier.height(28.dp))
                KeywordCard(store, actions.addKeyword, actions.removeKeyword)

                Spacer(Modifier.height(28.dp))
                Privacy()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun EnableCard(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PrimaryButton(stringResource(R.string.cta_enable), Palette.Ember, onClick = onClick)
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.cta_hint),
            style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlatformCard(
    rule: Rule,
    state: Store.PlatformState?,
    onLimit: (Int) -> Unit,
    onScope: (Scope) -> Unit,
) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rule.name,
                modifier = Modifier.weight(1f),
                style = TextStyle(color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium),
            )
            if (state != null) {
                Text(
                    stringResource(R.string.used_today, state.usedSec / 60),
                    style = TextStyle(color = Palette.Muted, fontSize = 14.sp),
                )
            }
        }
        if (!rule.supported || state == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.coming_soon),
                style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
            )
            return@Card
        }
        UsageBar(state)
        Spacer(Modifier.height(16.dp))
        ScopeSwitch(state.scope, onScope)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.limit_label, state.limitMin),
                modifier = Modifier.weight(1f),
                style = TextStyle(color = Palette.Text, fontSize = 16.sp),
            )
            RoundButton("−", stringResource(R.string.limit_less), state.limitMin > Store.MIN_LIMIT) {
                onLimit(state.limitMin - Store.LIMIT_STEP)
            }
            Spacer(Modifier.width(10.dp))
            RoundButton("+", stringResource(R.string.limit_more), state.limitMin < Store.MAX_LIMIT) {
                onLimit(state.limitMin + Store.LIMIT_STEP)
            }
        }
    }
}

/** 已用 / 可用 的進度條,快用完時由綠轉橘。 */
@Composable
internal fun UsageBar(state: Store.PlatformState) {
    val ratio by animateFloatAsState(
        (state.usedSec.toFloat() / state.allowedSec).coerceIn(0f, 1f), tween(500), label = "ratio",
    )
    val bar = if (ratio >= 0.85f) Palette.Ember else Palette.Lime
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Palette.Line)) {
        Box(Modifier.fillMaxWidth(ratio).height(8.dp).clip(CircleShape).background(bar))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordCard(store: Store.Snapshot, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val submit = {
        if (input.isNotBlank()) onAdd(input)
        input = ""
    }
    Card {
        Text(
            stringResource(R.string.section_words),
            style = TextStyle(color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.words_desc),
            style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
        )
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Palette.SurfaceHi)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { input = it.take(24) },
                    singleLine = true,
                    textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp),
                    cursorBrush = SolidColor(Palette.Lime),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text(
                                stringResource(R.string.words_hint),
                                style = TextStyle(color = Palette.Muted, fontSize = 16.sp),
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Palette.Lime)
                    .clickable(role = Role.Button) { submit() }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.words_add),
                    style = TextStyle(color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (store.keywords.isEmpty()) {
            Text(
                stringResource(R.string.words_empty),
                style = TextStyle(color = Palette.Muted, fontSize = 14.sp),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                store.keywords.forEach { word ->
                    val label = stringResource(R.string.words_remove, word)
                    Row(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clip(CircleShape)
                            .background(Palette.SurfaceHi)
                            .border(1.dp, Palette.Line, CircleShape)
                            .clickable(role = Role.Button) { onRemove(word) }
                            .semantics { contentDescription = label }
                            .padding(start = 18.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(word, style = TextStyle(color = Palette.Text, fontSize = 15.sp))
                        Spacer(Modifier.width(10.dp))
                        Text("×", style = TextStyle(color = Palette.Muted, fontSize = 18.sp))
                    }
                }
            }
        }

        if (store.skipped > 0) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.skipped_today, store.skipped),
                style = TextStyle(color = Palette.Lime, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}
