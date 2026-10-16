package org.limitshorts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R
import org.limitshorts.app.data.QuietHours
import org.limitshorts.app.data.Store
import org.limitshorts.app.service.DumpState

@Composable
fun SettingsScreen(
    store: Store.Snapshot,
    dump: DumpState.Snapshot,
    actions: AppActions,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Palette.Ink).systemBarsPadding()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            TopBar(onBack = onBack)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_title),
                style = TextStyle(color = Palette.Text, fontSize = 34.sp, fontWeight = FontWeight.Light),
            )
            Spacer(Modifier.height(24.dp))

            // 介面模式
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.set_mode_title),
                            style = TextStyle(color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.set_mode_desc),
                            style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    PillSwitch(store.simpleMode, actions.setSimpleMode, stringResource(R.string.set_mode_title))
                }
            }
            Spacer(Modifier.height(16.dp))

            // 休息時間
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.quiet_title),
                            style = TextStyle(color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.quiet_desc),
                            style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    PillSwitch(store.quiet.enabled, actions.setQuietEnabled, stringResource(R.string.quiet_title))
                }
                if (store.quiet.enabled) {
                    Spacer(Modifier.height(20.dp))
                    TimeStepper(stringResource(R.string.quiet_from), store.quiet.startMin, actions.setQuietStart)
                    Spacer(Modifier.height(14.dp))
                    TimeStepper(stringResource(R.string.quiet_to), store.quiet.endMin, actions.setQuietEnd)
                }
            }
            Spacer(Modifier.height(16.dp))

            // 語言(Android 13 以上才有 App 語言設定頁)
            actions.openLanguageSettings?.let { open ->
                Card(Modifier.clickable(role = Role.Button, onClick = open)) {
                    Text(
                        stringResource(R.string.lang_title),
                        style = TextStyle(color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.lang_desc),
                        style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // 開發者工具:簡易模式下不顯示,避免嚇到長輩
            if (!store.simpleMode) {
                DeveloperTools(dump, actions.toggleRecording, actions.clearDumps)
                Spacer(Modifier.height(16.dp))
            }

            Card {
                Text(
                    stringResource(R.string.about_title),
                    style = TextStyle(color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.about_text),
                    style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** 一個「標籤 + 大時間 + 早一點 / 晚一點」的列,每按一下動 30 分鐘。 */
@Composable
internal fun TimeStepper(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(56.dp), style = TextStyle(color = Palette.Muted, fontSize = 15.sp))
        Text(
            QuietHours.format(minutes),
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = Palette.Text, fontSize = 32.sp, fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
            ),
        )
        RoundButton("−", stringResource(R.string.time_earlier), true) { onChange(minutes - QuietHours.STEP) }
        Spacer(Modifier.width(10.dp))
        RoundButton("+", stringResource(R.string.time_later), true) { onChange(minutes + QuietHours.STEP) }
    }
}

@Composable
private fun DeveloperTools(dump: DumpState.Snapshot, onToggle: (Boolean) -> Unit, onClear: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Card(Modifier.clickable(role = Role.Button) { open = !open }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dev_title),
                        style = TextStyle(color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.dev_desc),
                        style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(if (open) "▴" else "▾", style = TextStyle(color = Palette.Muted, fontSize = 18.sp))
            }
        }
        if (open) {
            Spacer(Modifier.height(12.dp))
            DiagnosticCard(dump, onToggle, onClear)
        }
    }
}

@Composable
private fun DiagnosticCard(state: DumpState.Snapshot, onToggle: (Boolean) -> Unit, onClear: () -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.diag_title),
                modifier = Modifier.weight(1f),
                style = TextStyle(color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium),
            )
            PillSwitch(state.recording, onToggle, stringResource(R.string.diag_title))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.diag_desc),
            style = TextStyle(color = Palette.Muted, fontSize = 14.sp, lineHeight = 21.sp),
        )
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.count.toString(),
                    style = TextStyle(
                        color = if (state.recording) Palette.Lime else Palette.Text,
                        fontSize = 40.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.Monospace,
                    ),
                )
                Text(
                    stringResource(R.string.diag_count_label),
                    style = TextStyle(color = Palette.Muted, fontSize = 13.sp),
                )
            }
            if (state.count > 0) {
                Text(
                    stringResource(R.string.diag_clear),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClick = onClear)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    style = TextStyle(color = Palette.Ember, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
        state.lastPackage?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.diag_last, it),
                style = TextStyle(color = Palette.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.diag_pull),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.SurfaceHi)
                .padding(12.dp),
            style = TextStyle(
                color = Palette.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp,
            ),
        )
    }
}
