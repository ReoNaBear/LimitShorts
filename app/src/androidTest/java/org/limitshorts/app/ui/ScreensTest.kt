package org.limitshorts.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.limitshorts.app.R
import org.limitshorts.app.data.BlockReason
import org.limitshorts.app.data.QuietHours
import org.limitshorts.app.data.Scope
import org.limitshorts.app.data.Store

class ScreensTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /** 用 Activity 取字串,和畫面裡 stringResource 用的語言一致。 */
    private fun str(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    // ---------- 阻擋畫面 ----------

    @Test
    fun block_limit_rest_and_extend_call_back() {
        val calls = mutableListOf<String>()
        rule.setContent {
            BlockScreen(BlockReason.LIMIT, "YouTube", 3, "22:00", "07:00", { calls += "rest" }, { calls += "extend" })
        }
        rule.onNodeWithText(str(R.string.block_title)).assertExists()
        rule.onNodeWithText(str(R.string.block_extend_left, 3)).assertExists()
        rule.onNodeWithText(str(R.string.block_extend, Store.EXT_MINUTES)).performClick()
        rule.onNodeWithText(str(R.string.block_rest)).performClick()
        assertEquals(listOf("extend", "rest"), calls)
    }

    @Test
    fun block_limit_extend_is_disabled_when_none_left() {
        val calls = mutableListOf<String>()
        rule.setContent {
            BlockScreen(BlockReason.LIMIT, "YouTube", 0, "22:00", "07:00", { calls += "rest" }, { calls += "extend" })
        }
        rule.onNodeWithText(str(R.string.block_extend, Store.EXT_MINUTES)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.block_extend_none)).assertExists()
        rule.onNodeWithText(str(R.string.block_extend, Store.EXT_MINUTES)).performClick()
        assertTrue("extend must not fire when disabled", calls.isEmpty())
    }

    @Test
    fun block_quiet_has_no_extend_and_shows_the_times() {
        rule.setContent {
            BlockScreen(BlockReason.QUIET, "YouTube", 3, "22:00", "07:00", {}, {})
        }
        rule.onNodeWithText(str(R.string.block_quiet_title)).assertExists()
        rule.onNodeWithText(str(R.string.block_quiet_message, "22:00", "07:00")).assertExists()
        rule.onAllNodesWithText(str(R.string.block_extend, Store.EXT_MINUTES)).assertCountEquals0()
        rule.onNodeWithText(str(R.string.block_rest)).assertIsEnabled()
    }

    // ---------- 標準首頁 ----------

    @Test
    fun home_off_shows_enable_button() {
        val rec = Recorder()
        rule.setContent { HomeScreen(allRules, snapshot(), false, rec.actions) {} }
        rule.onNodeWithText(str(R.string.cta_enable)).performClick()
        assertEquals(listOf("openAccessibility"), rec.calls)
    }

    @Test
    fun home_on_shows_usage_and_limit() {
        rule.setContent { HomeScreen(allRules, snapshot(usedSec = 600, limitMin = 30), true, Recorder().actions) {} }
        rule.onNodeWithText(str(R.string.used_today, 10)).assertExists()
        rule.onNodeWithText(str(R.string.limit_label, 30)).assertExists()
        rule.onNodeWithText(str(R.string.week_title)).assertExists()
    }

    @Test
    fun home_unsupported_platforms_say_coming_soon() {
        rule.setContent { HomeScreen(allRules, snapshot(), true, Recorder().actions) {} }
        assertEquals(2, rule.onAllNodesWithText(str(R.string.coming_soon)).fetchSemanticsNodes().size)
    }

    @Test
    fun home_limit_buttons_step_by_five() {
        val rec = Recorder()
        rule.setContent { HomeScreen(allRules, snapshot(limitMin = 30), true, rec.actions) {} }
        rule.onNodeWithContentDescription(str(R.string.limit_more)).performClick()
        rule.onNodeWithContentDescription(str(R.string.limit_less)).performClick()
        assertEquals(listOf("setLimit:youtube:35", "setLimit:youtube:25"), rec.calls)
    }

    @Test
    fun home_limit_minus_is_disabled_at_the_minimum() {
        rule.setContent { HomeScreen(allRules, snapshot(limitMin = Store.MIN_LIMIT), true, Recorder().actions) {} }
        rule.onNodeWithContentDescription(str(R.string.limit_less)).assertIsNotEnabled()
    }

    @Test
    fun home_limit_plus_is_disabled_at_the_maximum() {
        rule.setContent { HomeScreen(allRules, snapshot(limitMin = Store.MAX_LIMIT), true, Recorder().actions) {} }
        rule.onNodeWithContentDescription(str(R.string.limit_more)).assertIsNotEnabled()
    }

    @Test
    fun home_can_add_and_remove_keywords() {
        val rec = Recorder()
        rule.setContent { HomeScreen(allRules, snapshot(keywords = listOf("AI")), true, rec.actions) {} }
        rule.onNode(hasSetTextAction()).performTextInput("貓咪")
        rule.onNodeWithText(str(R.string.words_add)).performClick()
        // 標籤在捲動區域下方,要先捲到看得見,點擊才會落在畫面內
        rule.onNodeWithContentDescription(str(R.string.words_remove, "AI")).performScrollTo().performClick()
        assertEquals(listOf("addKeyword:貓咪", "removeKeyword:AI"), rec.calls)
    }

    @Test
    fun home_blank_keyword_is_not_added() {
        val rec = Recorder()
        rule.setContent { HomeScreen(allRules, snapshot(), true, rec.actions) {} }
        rule.onNode(hasSetTextAction()).performTextInput("   ")
        rule.onNodeWithText(str(R.string.words_add)).performClick()
        assertTrue(rec.calls.isEmpty())
    }

    @Test
    fun home_shows_skipped_count_only_when_positive() {
        rule.setContent { HomeScreen(allRules, snapshot(skipped = 4), true, Recorder().actions) {} }
        rule.onNodeWithText(str(R.string.skipped_today, 4)).assertExists()
    }

    @Test
    fun home_settings_button_opens_settings() {
        var opened = false
        rule.setContent { HomeScreen(allRules, snapshot(), true, Recorder().actions) { opened = true } }
        rule.onNodeWithContentDescription(str(R.string.settings_open_desc)).performClick()
        assertTrue(opened)
    }

    // ---------- 簡易首頁 ----------

    @Test
    fun simple_home_shows_big_limit_and_steps_it() {
        val rec = Recorder()
        rule.setContent { SimpleHomeScreen(allRules, snapshot(limitMin = 30, simple = true), true, rec.actions) {} }
        rule.onNodeWithText(str(R.string.simple_limit_title)).assertExists()
        rule.onNodeWithText("30").assertExists()
        rule.onNodeWithContentDescription(str(R.string.limit_more)).performClick()
        assertEquals(listOf("setLimit:youtube:35"), rec.calls)
    }

    @Test
    fun simple_home_hides_keywords_and_unfinished_platforms() {
        rule.setContent { SimpleHomeScreen(allRules, snapshot(simple = true), true, Recorder().actions) {} }
        rule.onAllNodesWithText(str(R.string.section_words)).assertCountEquals0()
        rule.onAllNodesWithText(str(R.string.coming_soon)).assertCountEquals0()
    }

    @Test
    fun simple_home_quiet_switch_toggles() {
        val rec = Recorder()
        rule.setContent { SimpleHomeScreen(allRules, snapshot(simple = true), true, rec.actions) {} }
        rule.onNode(isToggleable()).performClick()
        assertEquals(listOf("setQuietEnabled:true"), rec.calls)
    }

    @Test
    fun simple_home_off_state_has_a_big_enable_button() {
        val rec = Recorder()
        rule.setContent { SimpleHomeScreen(allRules, snapshot(simple = true), false, rec.actions) {} }
        rule.onNodeWithText(str(R.string.cta_enable)).performClick()
        assertEquals(listOf("openAccessibility"), rec.calls)
    }

    // ---------- 設定 ----------

    @Test
    fun settings_mode_switch_calls_back() {
        val rec = Recorder()
        rule.setContent { SettingsScreen(snapshot(simple = false), noDump, rec.actions) {} }
        rule.onNodeWithContentDescription(str(R.string.set_mode_title)).performClick()
        assertEquals(listOf("setSimpleMode:true"), rec.calls)
    }

    @Test
    fun settings_quiet_time_steppers_only_show_when_enabled() {
        rule.setContent { SettingsScreen(snapshot(quiet = QuietHours(enabled = false)), noDump, Recorder().actions) {} }
        rule.onAllNodesWithContentDescription(str(R.string.time_later)).assertCountEquals0()
    }

    @Test
    fun settings_quiet_time_moves_by_thirty_minutes() {
        val rec = Recorder()
        val quiet = QuietHours(enabled = true, startMin = 22 * 60, endMin = 7 * 60)
        rule.setContent { SettingsScreen(snapshot(quiet = quiet), noDump, rec.actions) {} }
        rule.onNodeWithText("22:00").assertExists()
        rule.onNodeWithText("07:00").assertExists()
        rule.onAllNodesWithContentDescription(str(R.string.time_later))[0].performClick()
        rule.onAllNodesWithContentDescription(str(R.string.time_earlier))[1].performClick()
        assertEquals(listOf("setQuietStart:${22 * 60 + 30}", "setQuietEnd:${7 * 60 - 30}"), rec.calls)
    }

    @Test
    fun settings_language_row_calls_back_when_available() {
        val rec = Recorder()
        rule.setContent { SettingsScreen(snapshot(), noDump, rec.actions) {} }
        rule.onNodeWithText(str(R.string.lang_title)).performClick()
        assertEquals(listOf("openLanguageSettings"), rec.calls)
    }

    @Test
    fun settings_developer_tools_are_hidden_in_simple_mode() {
        rule.setContent { SettingsScreen(snapshot(simple = true), noDump, Recorder().actions) {} }
        rule.onAllNodesWithText(str(R.string.dev_title), substring = true).assertCountEquals0()
    }

    @Test
    fun settings_developer_tools_open_and_toggle_recording() {
        val rec = Recorder()
        rule.setContent { SettingsScreen(snapshot(simple = false), noDump, rec.actions) {} }
        rule.onNodeWithText(str(R.string.dev_title), substring = true).performClick()
        rule.onNodeWithContentDescription(str(R.string.diag_title)).performClick()
        assertEquals(listOf("toggleRecording:true"), rec.calls)
    }

    @Test
    fun settings_back_button_goes_back() {
        var back = false
        rule.setContent { SettingsScreen(snapshot(), noDump, Recorder().actions) { back = true } }
        rule.onNodeWithContentDescription(str(R.string.back_desc)).performClick()
        assertTrue(back)
    }

    // ---------- 引導 ----------

    @Test
    fun onboarding_welcome_next() {
        var next = 0
        rule.setContent { OnboardingScreen(0, { next++ }, {}, {}, {}) }
        rule.onNodeWithText(str(R.string.onb_welcome_title)).assertExists()
        rule.onNodeWithText(str(R.string.onb_next)).performClick()
        assertEquals(1, next)
    }

    @Test
    fun onboarding_mode_cards_report_the_choice() {
        val picks = mutableListOf<Boolean>()
        rule.setContent { OnboardingScreen(1, {}, { picks += it }, {}, {}) }
        rule.onNodeWithText(str(R.string.onb_mode_simple)).performClick()
        rule.onNodeWithText(str(R.string.onb_mode_standard)).performClick()
        assertEquals(listOf(true, false), picks)
    }

    @Test
    fun onboarding_last_step_opens_settings_or_skips() {
        val calls = mutableListOf<String>()
        rule.setContent { OnboardingScreen(2, {}, {}, { calls += "open" }, { calls += "skip" }) }
        rule.onNodeWithText(str(R.string.onb_enable_btn)).performClick()
        rule.onNodeWithText(str(R.string.onb_later)).performClick()
        assertEquals(listOf("open", "skip"), calls)
    }

    // ---------- 計時範圍(只算短影片 / 整個 App) ----------

    @Test
    fun scope_default_selects_short_videos_only() {
        rule.setContent { HomeScreen(allRules, snapshot(), true, Recorder().actions) {} }
        rule.onNodeWithText(str(R.string.scope_shorts)).assertIsSelected()
        rule.onNodeWithText(str(R.string.scope_app)).assertIsNotSelected()
    }

    @Test
    fun scope_reflects_the_stored_choice() {
        rule.setContent { HomeScreen(allRules, snapshot(scope = Scope.APP), true, Recorder().actions) {} }
        rule.onNodeWithText(str(R.string.scope_app)).assertIsSelected()
        rule.onNodeWithText(str(R.string.scope_shorts)).assertIsNotSelected()
    }

    @Test
    fun scope_tapping_whole_app_reports_the_platform_and_choice() {
        val rec = Recorder()
        rule.setContent { HomeScreen(allRules, snapshot(), true, rec.actions) {} }
        rule.onNodeWithText(str(R.string.scope_app)).performClick()
        assertEquals(listOf("setScope:youtube:APP"), rec.calls)
    }

    @Test
    fun scope_tapping_short_videos_reports_the_choice() {
        val rec = Recorder()
        rule.setContent { HomeScreen(allRules, snapshot(scope = Scope.APP), true, rec.actions) {} }
        rule.onNodeWithText(str(R.string.scope_shorts)).performClick()
        assertEquals(listOf("setScope:youtube:SHORTS"), rec.calls)
    }

    @Test
    fun scope_is_also_available_on_the_simple_screen() {
        val rec = Recorder()
        rule.setContent { SimpleHomeScreen(allRules, snapshot(simple = true), true, rec.actions) {} }
        rule.onNodeWithText(str(R.string.scope_shorts)).assertIsSelected()
        rule.onNodeWithText(str(R.string.scope_app)).performClick()
        assertEquals(listOf("setScope:youtube:APP"), rec.calls)
    }

    @Test
    fun scope_is_not_offered_for_platforms_that_are_not_ready() {
        rule.setContent { HomeScreen(allRules, snapshot(), true, Recorder().actions) {} }
        // fixtures: only YouTube is supported, so exactly one switch (two options) exists
        assertEquals(1, rule.onAllNodesWithText(str(R.string.scope_app)).fetchSemanticsNodes().size)
    }

    // ---------- 整個 App 的流程 ----------

    @Test
    fun app_first_run_walks_through_onboarding() {
        val rec = Recorder()
        rule.setContent { AppRoot(allRules, snapshot(onboarded = false), noDump, false, rec.actions) }
        rule.onNodeWithText(str(R.string.onb_welcome_title)).assertExists()
        rule.onNodeWithText(str(R.string.onb_next)).performClick()
        rule.onNodeWithText(str(R.string.onb_mode_simple)).performClick()
        rule.onNodeWithText(str(R.string.onb_enable_title)).assertExists()
        rule.onNodeWithText(str(R.string.onb_later)).performClick()
        assertEquals(listOf("setSimpleMode:true", "finishOnboarding"), rec.calls)
    }

    @Test
    fun app_onboarding_finishes_by_itself_once_the_service_is_on() {
        val rec = Recorder()
        rule.setContent { AppRoot(allRules, snapshot(onboarded = false), noDump, true, rec.actions) }
        rule.onNodeWithText(str(R.string.onb_next)).performClick()
        rule.onNodeWithText(str(R.string.onb_mode_standard)).performClick()
        rule.waitForIdle()
        assertTrue("finishOnboarding" in rec.calls)
    }

    @Test
    fun app_returning_user_goes_straight_to_home_and_can_visit_settings() {
        rule.setContent { AppRoot(allRules, snapshot(), noDump, true, Recorder().actions) }
        rule.onNodeWithText(str(R.string.section_platforms)).assertExists()
        rule.onNodeWithContentDescription(str(R.string.settings_open_desc)).performClick()
        rule.onNodeWithText(str(R.string.settings_title)).assertExists()
        rule.onNodeWithContentDescription(str(R.string.back_desc)).performClick()
        rule.onNodeWithText(str(R.string.section_platforms)).assertExists()
    }

    @Test
    fun app_simple_mode_uses_the_simple_home() {
        rule.setContent { AppRoot(allRules, snapshot(simple = true), noDump, true, Recorder().actions) }
        rule.onNodeWithText(str(R.string.simple_limit_title)).assertExists()
        rule.onAllNodesWithText(str(R.string.section_words)).assertCountEquals0()
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals0() {
    assertEquals(0, fetchSemanticsNodes(atLeastOneRootRequired = false).size)
}
