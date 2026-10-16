package org.limitshorts.app.ui

import org.limitshorts.app.data.Scope

/** 畫面會用到的所有動作。畫面只負責呼叫,真正的邏輯在 Store 與服務裡,所以畫面好測。 */
class AppActions(
    val openAccessibility: () -> Unit,
    val toggleRecording: (Boolean) -> Unit,
    val clearDumps: () -> Unit,
    val setLimit: (platformId: String, minutes: Int) -> Unit,
    val setScope: (platformId: String, scope: Scope) -> Unit,
    val addKeyword: (String) -> Unit,
    val removeKeyword: (String) -> Unit,
    val setSimpleMode: (Boolean) -> Unit,
    val setQuietEnabled: (Boolean) -> Unit,
    val setQuietStart: (Int) -> Unit,
    val setQuietEnd: (Int) -> Unit,
    val finishOnboarding: () -> Unit,
    /** Android 13 以上才能從 App 內跳到「App 語言」設定;其他版本是 null。 */
    val openLanguageSettings: (() -> Unit)? = null,
)
