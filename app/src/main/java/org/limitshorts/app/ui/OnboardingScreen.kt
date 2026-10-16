package org.limitshorts.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.limitshorts.app.R

const val ONBOARDING_STEPS = 3

/**
 * 第一次打開時的引導,三步:認識它 → 選畫面 → 打開無障礙服務。
 * step 與動作都由外面傳進來,方便測試。
 */
@Composable
fun OnboardingScreen(
    step: Int,
    onNext: () -> Unit,
    onPickMode: (simple: Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onSkip: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Ink).systemBarsPadding()) {
        val minHeight = maxHeight
        Crossfade(targetState = step, animationSpec = tween(350), label = "onboarding") { s ->
            // 三段式:點點在上、內容在中、主要動作貼底(拇指好按)。內容太長時整頁可以捲動。
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                StepDots(s)
                when (s) {
                    0 -> WelcomeContent()
                    1 -> ModeContent(onPickMode)
                    else -> EnableContent()
                }
                when (s) {
                    0 -> PrimaryButton(
                        stringResource(R.string.onb_next), Palette.Lime,
                        height = 68.dp, textSize = 20, onClick = onNext,
                    )

                    1 -> Text(
                        stringResource(R.string.onb_mode_note),
                        modifier = Modifier.padding(vertical = 20.dp),
                        style = TextStyle(color = Palette.Muted, fontSize = 15.sp),
                        textAlign = TextAlign.Center,
                    )

                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PrimaryButton(
                            stringResource(R.string.onb_enable_btn), Palette.Ember,
                            height = 68.dp, textSize = 20, onClick = onOpenAccessibility,
                        )
                        Spacer(Modifier.height(8.dp))
                        GhostButton(stringResource(R.string.onb_later), onClick = onSkip)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    val desc = stringResource(R.string.onb_step_desc, step + 1, ONBOARDING_STEPS)
    Row(
        Modifier.semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(ONBOARDING_STEPS) { i ->
            val color by animateColorAsState(if (i == step) Palette.Lime else Palette.Line, tween(300), label = "dot")
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (i == step) 28.dp else 6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun StepTitle(text: String) = Text(
    text,
    style = TextStyle(color = Palette.Text, fontSize = 36.sp, fontWeight = FontWeight.Light, lineHeight = 46.sp),
    textAlign = TextAlign.Center,
)

@Composable
private fun WelcomeContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepTitle(stringResource(R.string.onb_welcome_title))
        Spacer(Modifier.height(40.dp))
        NumberedLine(1, stringResource(R.string.onb_welcome_1))
        Spacer(Modifier.height(20.dp))
        NumberedLine(2, stringResource(R.string.onb_welcome_2))
        Spacer(Modifier.height(20.dp))
        NumberedLine(3, stringResource(R.string.onb_welcome_3))
    }
}

@Composable
private fun ModeContent(onPick: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepTitle(stringResource(R.string.onb_mode_title))
        Spacer(Modifier.height(36.dp))
        ModeCard(
            stringResource(R.string.onb_mode_simple), stringResource(R.string.onb_mode_simple_desc),
            Palette.Lime,
        ) { onPick(true) }
        Spacer(Modifier.height(16.dp))
        ModeCard(
            stringResource(R.string.onb_mode_standard), stringResource(R.string.onb_mode_standard_desc),
            Palette.Muted,
        ) { onPick(false) }
    }
}

@Composable
private fun ModeCard(title: String, desc: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, style = TextStyle(color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(6.dp))
            Text(desc, style = TextStyle(color = Palette.Muted, fontSize = 15.sp, lineHeight = 22.sp))
        }
    }
}

@Composable
private fun EnableContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepTitle(stringResource(R.string.onb_enable_title))
        Spacer(Modifier.height(40.dp))
        NumberedLine(1, stringResource(R.string.onb_enable_1))
        Spacer(Modifier.height(20.dp))
        NumberedLine(2, stringResource(R.string.onb_enable_2))
        Spacer(Modifier.height(20.dp))
        NumberedLine(3, stringResource(R.string.onb_enable_3))
    }
}

@Composable
private fun NumberedLine(n: Int, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).border(1.dp, Palette.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(n.toString(), style = TextStyle(color = Palette.Lime, fontSize = 16.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = Palette.Text, fontSize = 19.sp, lineHeight = 27.sp),
        )
    }
}
