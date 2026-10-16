package org.limitshorts.app.ui

import org.limitshorts.app.data.QuietHours
import org.limitshorts.app.data.Rule
import org.limitshorts.app.data.Scope
import org.limitshorts.app.data.Store
import org.limitshorts.app.service.DumpState
import java.time.LocalDate

val youtubeRule = Rule("youtube", "YouTube", "com.google.android.youtube", listOf("reel_recycler"), emptyList(), 0f, 0.12f)
val instagramRule = Rule("instagram", "Instagram", "com.instagram.android", emptyList(), emptyList(), 0f, 0.12f)
val facebookRule = Rule("facebook", "Facebook", "com.facebook.katana", emptyList(), emptyList(), 0f, 0f)
val allRules = listOf(youtubeRule, instagramRule, facebookRule)

fun snapshot(
    usedSec: Int = 600,
    limitMin: Int = 30,
    ext: Int = 0,
    scope: Scope = Scope.SHORTS,
    keywords: List<String> = emptyList(),
    skipped: Int = 0,
    quiet: QuietHours = QuietHours(),
    simple: Boolean = false,
    onboarded: Boolean = true,
) = Store.Snapshot(
    platforms = mapOf("youtube" to Store.PlatformState("youtube", usedSec, limitMin, ext, scope)),
    keywords = keywords,
    skipped = skipped,
    quiet = quiet,
    simpleMode = simple,
    onboarded = onboarded,
    week = (6 downTo 0).map { Store.DayTotal(LocalDate.of(2026, 10, 15).minusDays(it.toLong()), it * 300) },
)

/** 記下每個動作被呼叫的情形,測試時再檢查。 */
class Recorder {
    val calls = mutableListOf<String>()

    val actions = AppActions(
        openAccessibility = { calls += "openAccessibility" },
        toggleRecording = { calls += "toggleRecording:$it" },
        clearDumps = { calls += "clearDumps" },
        setLimit = { id, m -> calls += "setLimit:$id:$m" },
        setScope = { id, sc -> calls += "setScope:$id:${sc.name}" },
        addKeyword = { calls += "addKeyword:$it" },
        removeKeyword = { calls += "removeKeyword:$it" },
        setSimpleMode = { calls += "setSimpleMode:$it" },
        setQuietEnabled = { calls += "setQuietEnabled:$it" },
        setQuietStart = { calls += "setQuietStart:$it" },
        setQuietEnd = { calls += "setQuietEnd:$it" },
        finishOnboarding = { calls += "finishOnboarding" },
        openLanguageSettings = { calls += "openLanguageSettings" },
    )
}

val noDump = DumpState.Snapshot()
