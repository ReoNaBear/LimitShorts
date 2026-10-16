package org.limitshorts.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.limitshorts.app.data.Rule
import org.limitshorts.app.data.Store
import org.limitshorts.app.service.DumpState

private const val SCREEN_ONBOARDING = 0
private const val SCREEN_HOME = 1
private const val SCREEN_SETTINGS = 2

/** 決定現在要顯示哪個畫面:第一次 → 引導;之後 → 首頁(依模式),可前往設定。 */
@Composable
fun AppRoot(
    rules: List<Rule>,
    store: Store.Snapshot,
    dump: DumpState.Snapshot,
    serviceEnabled: Boolean,
    actions: AppActions,
) {
    var screen by rememberSaveable { mutableIntStateOf(if (store.onboarded) SCREEN_HOME else SCREEN_ONBOARDING) }
    var step by rememberSaveable { mutableIntStateOf(0) }

    val finishOnboarding = {
        actions.finishOnboarding()
        screen = SCREEN_HOME
    }

    // 在最後一步把服務打開後,自動結束引導
    LaunchedEffect(serviceEnabled, screen, step) {
        if (screen == SCREEN_ONBOARDING && step == ONBOARDING_STEPS - 1 && serviceEnabled) finishOnboarding()
    }

    BackHandler(enabled = screen == SCREEN_SETTINGS) { screen = SCREEN_HOME }
    BackHandler(enabled = screen == SCREEN_ONBOARDING && step > 0) { step -= 1 }

    Crossfade(targetState = screen, animationSpec = tween(250), label = "screen") { current ->
        when (current) {
            SCREEN_ONBOARDING -> OnboardingScreen(
                step = step,
                onNext = { step += 1 },
                onPickMode = {
                    actions.setSimpleMode(it)
                    step = ONBOARDING_STEPS - 1
                },
                onOpenAccessibility = actions.openAccessibility,
                onSkip = finishOnboarding,
            )

            SCREEN_SETTINGS -> SettingsScreen(store, dump, actions) { screen = SCREEN_HOME }

            else -> if (store.simpleMode) {
                SimpleHomeScreen(rules, store, serviceEnabled, actions) { screen = SCREEN_SETTINGS }
            } else {
                HomeScreen(rules, store, serviceEnabled, actions) { screen = SCREEN_SETTINGS }
            }
        }
    }
}
