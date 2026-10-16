package org.limitshorts.app.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R
import org.limitshorts.app.data.Scope

/** 不依賴 Material 的最小文字元件,樣式全部由 TextStyle 決定。 */
@Composable
internal fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    textAlign: TextAlign? = null,
) = BasicText(
    text = text,
    modifier = modifier,
    style = if (textAlign != null) style.copy(textAlign = textAlign) else style,
)

@Composable
internal fun Brand() = Text(
    stringResource(R.string.brand),
    style = TextStyle(
        color = Palette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp,
    ),
)

@Composable
internal fun Privacy() = Text(
    stringResource(R.string.privacy),
    style = TextStyle(color = Palette.Muted, fontSize = 12.sp, letterSpacing = 0.5.sp, lineHeight = 18.sp),
    textAlign = TextAlign.Center,
)

@Composable
internal fun SectionTitle(text: String, size: Int = 22) = Text(
    text,
    modifier = Modifier.fillMaxWidth(),
    style = TextStyle(color = Palette.Text, fontSize = size.sp, fontWeight = FontWeight.Medium),
)

@Composable
internal fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) { content() }
}

@Composable
internal fun Pill(text: String, color: Color) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, style = TextStyle(color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
internal fun PrimaryButton(
    text: String,
    background: Color,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    textSize: Int = 18,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(color = Palette.Ink, fontSize = textSize.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
internal fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(color = Palette.Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
internal fun RoundButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    size: Dp = 48.dp,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.SurfaceHi)
            .border(1.dp, Palette.Line, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = TextStyle(
                color = if (enabled) Palette.Text else Palette.Muted.copy(alpha = 0.4f),
                fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.Light,
            ),
        )
    }
}

@Composable
internal fun PillSwitch(checked: Boolean, onChange: (Boolean) -> Unit, label: String) {
    val t = updateTransition(checked, label = "switch")
    val track by t.animateColor(label = "track") { if (it) Palette.Lime else Palette.Line }
    val thumb by t.animateColor(label = "thumb") { if (it) Palette.Ink else Palette.Muted }
    val x by t.animateDp(label = "x") { if (it) 26.dp else 4.dp }

    // 外層是 64x48 的可點範圍(符合 48dp 最小觸控尺寸),裡面的軌道仍是 60x34
    Box(
        Modifier
            .size(width = 64.dp, height = 48.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 60.dp, height = 34.dp)
                .clip(CircleShape)
                .background(track),
        ) {
            Box(Modifier.offset(x = x, y = 4.dp).size(26.dp).clip(CircleShape).background(thumb))
        }
    }
}

/** 頂端列:中間是品牌字;左邊可放返回、右邊可放設定。 */
@Composable
internal fun TopBar(onBack: (() -> Unit)? = null, onSettings: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        Brand()
        if (onBack != null) {
            IconTap(
                stringResource(R.string.back_desc), onBack,
                Modifier.align(Alignment.CenterStart),
            ) { drawBack() }
        }
        if (onSettings != null) {
            IconTap(
                stringResource(R.string.settings_open_desc), onSettings,
                Modifier.align(Alignment.CenterEnd),
            ) { drawSliders() }
        }
    }
}

@Composable
private fun IconTap(description: String, onClick: () -> Unit, modifier: Modifier, draw: DrawScope.() -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) { draw() }
    }
}

private fun DrawScope.drawBack() {
    val w = size.width
    val h = size.height
    val s = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Palette.Text, Offset(w * 0.65f, h * 0.15f), Offset(w * 0.3f, h * 0.5f), s.width, StrokeCap.Round)
    drawLine(Palette.Text, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.65f, h * 0.85f), s.width, StrokeCap.Round)
}

/** 兩條帶旋鈕的橫線,通用的「調整」符號。 */
private fun DrawScope.drawSliders() {
    val w = size.width
    val h = size.height
    val line = 2.dp.toPx()
    drawLine(Palette.Text, Offset(0f, h * 0.3f), Offset(w, h * 0.3f), line, StrokeCap.Round)
    drawLine(Palette.Text, Offset(0f, h * 0.7f), Offset(w, h * 0.7f), line, StrokeCap.Round)
    drawCircle(Palette.Ink, h * 0.15f, Offset(w * 0.68f, h * 0.3f))
    drawCircle(Palette.Text, h * 0.15f, Offset(w * 0.68f, h * 0.3f), style = Stroke(line))
    drawCircle(Palette.Ink, h * 0.15f, Offset(w * 0.32f, h * 0.7f))
    drawCircle(Palette.Text, h * 0.15f, Offset(w * 0.32f, h * 0.7f), style = Stroke(line))
}

/**
 * 兩選一的切換:「只算短影片」/「整個 App」。用 RadioButton 的語意,讀屏軟體會念出「已選取」。
 * big = true 時給簡單畫面用,字更大、更高。
 */
@Composable
internal fun ScopeSwitch(selected: Scope, onSelect: (Scope) -> Unit, big: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Palette.SurfaceHi)
            .border(1.dp, Palette.Line, CircleShape)
            .padding(4.dp),
    ) {
        ScopeOption(stringResource(R.string.scope_shorts), selected == Scope.SHORTS, big, Modifier.weight(1f)) {
            onSelect(Scope.SHORTS)
        }
        ScopeOption(stringResource(R.string.scope_app), selected == Scope.APP, big, Modifier.weight(1f)) {
            onSelect(Scope.APP)
        }
    }
}

@Composable
private fun ScopeOption(label: String, selected: Boolean, big: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(if (big) 56.dp else 48.dp)
            .clip(CircleShape)
            .background(if (selected) Palette.Lime else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                color = if (selected) Palette.Ink else Palette.Muted,
                fontSize = if (big) 17.sp else 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            textAlign = TextAlign.Center,
        )
    }
}
