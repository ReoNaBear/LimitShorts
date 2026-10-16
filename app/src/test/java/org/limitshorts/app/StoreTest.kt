package org.limitshorts.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.limitshorts.app.data.QuietHours
import org.limitshorts.app.data.Scope
import org.limitshorts.app.data.Store
import java.time.LocalDate

class StoreTest {
    private lateinit var prefs: FakePrefs
    private var today = LocalDate.of(2026, 10, 15)

    private fun open(ids: List<String> = listOf("youtube")) = Store.setup(prefs, ids, today = { today })
    private val yt get() = Store.state.value.platforms.getValue("youtube")

    @Before
    fun setUp() {
        prefs = FakePrefs()
        today = LocalDate.of(2026, 10, 15)
        open()
    }

    @Test
    fun `usage accumulates per platform`() {
        Store.addUsage("youtube", 1)
        Store.addUsage("youtube", 59)
        assertEquals(60, yt.usedSec)
    }

    @Test
    fun `default limit is thirty minutes`() {
        assertEquals(Store.DEFAULT_LIMIT, yt.limitMin)
    }

    @Test
    fun `limit is clamped to the allowed range`() {
        Store.setLimit("youtube", 1)
        assertEquals(Store.MIN_LIMIT, yt.limitMin)
        Store.setLimit("youtube", 9999)
        assertEquals(Store.MAX_LIMIT, yt.limitMin)
        Store.setLimit("youtube", 45)
        assertEquals(45, yt.limitMin)
    }

    @Test
    fun `extend works three times then refuses`() {
        assertTrue(Store.extend("youtube"))
        assertTrue(Store.extend("youtube"))
        assertTrue(Store.extend("youtube"))
        assertFalse(Store.extend("youtube"))
        assertEquals(3, yt.extUsed)
        assertEquals(0, yt.extLeft)
    }

    @Test
    fun `a new day resets usage extensions and skips but keeps settings`() {
        Store.setLimit("youtube", 45)
        Store.addKeyword("AI")
        Store.setQuietEnabled(true)
        Store.addUsage("youtube", 500)
        Store.extend("youtube")
        Store.addSkipped()

        today = today.plusDays(1)
        open()

        assertEquals(0, yt.usedSec)
        assertEquals(0, yt.extUsed)
        assertEquals(0, Store.state.value.skipped)
        assertEquals(45, yt.limitMin)
        assertEquals(listOf("AI"), Store.state.value.keywords)
        assertTrue(Store.state.value.quiet.enabled)
    }

    @Test
    fun `the first call on a new day also rolls over`() {
        Store.addUsage("youtube", 100)
        today = today.plusDays(1)
        Store.addUsage("youtube", 7)
        assertEquals(7, yt.usedSec)
    }

    @Test
    fun `keywords are trimmed deduplicated and removable`() {
        Store.addKeyword("  AI ")
        Store.addKeyword("ai")
        Store.addKeyword("")
        Store.addKeyword("   ")
        Store.addKeyword("人工智慧")
        assertEquals(listOf("AI", "人工智慧"), Store.state.value.keywords)
        Store.removeKeyword("AI")
        assertEquals(listOf("人工智慧"), Store.state.value.keywords)
    }

    @Test
    fun `skipped count goes up`() {
        Store.addSkipped()
        Store.addSkipped()
        assertEquals(2, Store.state.value.skipped)
    }

    @Test
    fun `quiet hours defaults and setters`() {
        assertEquals(QuietHours(enabled = false, startMin = 22 * 60, endMin = 7 * 60), Store.state.value.quiet)
        Store.setQuietEnabled(true)
        Store.setQuietStart(23 * 60 + 30)
        Store.setQuietEnd(6 * 60)
        assertEquals(QuietHours(true, 23 * 60 + 30, 6 * 60), Store.state.value.quiet)
    }

    @Test
    fun `quiet times wrap around midnight`() {
        Store.setQuietStart(-30)
        assertEquals(QuietHours.DAY - 30, Store.state.value.quiet.startMin)
        Store.setQuietEnd(QuietHours.DAY + 30)
        assertEquals(30, Store.state.value.quiet.endMin)
    }

    @Test
    fun `simple mode and onboarding flags persist`() {
        assertFalse(Store.state.value.simpleMode)
        assertFalse(Store.state.value.onboarded)
        Store.setSimpleMode(true)
        Store.setOnboarded(true)
        open() // 重新讀取,確認真的寫進儲存空間
        assertTrue(Store.state.value.simpleMode)
        assertTrue(Store.state.value.onboarded)
    }

    @Test
    fun `week has seven days ending today`() {
        val week = Store.state.value.week
        assertEquals(7, week.size)
        assertEquals(today, week.last().date)
        assertEquals(today.minusDays(6), week.first().date)
    }

    @Test
    fun `week totals add up across days`() {
        Store.addUsage("youtube", 120)
        today = today.plusDays(1)
        Store.addUsage("youtube", 30)

        val week = Store.state.value.week
        assertEquals(30, week.last().seconds)
        assertEquals(120, week[week.size - 2].seconds)
    }

    @Test
    fun `history older than seven days is pruned`() {
        Store.addUsage("youtube", 50)
        val firstDay = today
        today = today.plusDays(10)
        open()
        assertFalse(prefs.contains("day_$firstDay"))
        assertEquals(0, Store.state.value.week.sumOf { it.seconds })
    }

    @Test
    fun `week keeps days that are still inside the window`() {
        Store.addUsage("youtube", 50)
        today = today.plusDays(3)
        open()
        assertEquals(50, Store.state.value.week.sumOf { it.seconds })
    }

    @Test
    fun `scope defaults to short videos only`() {
        assertEquals(Scope.SHORTS, yt.scope)
    }

    @Test
    fun `scope can be switched to the whole app and survives a restart`() {
        Store.setScope("youtube", Scope.APP)
        assertEquals(Scope.APP, yt.scope)
        open()
        assertEquals(Scope.APP, yt.scope)
    }

    @Test
    fun `scope is kept per platform`() {
        open(listOf("youtube", "instagram", "facebook"))
        Store.setScope("instagram", Scope.APP)
        val p = Store.state.value.platforms
        assertEquals(Scope.SHORTS, p.getValue("youtube").scope)
        assertEquals(Scope.APP, p.getValue("instagram").scope)
        assertEquals(Scope.SHORTS, p.getValue("facebook").scope)
    }

    @Test
    fun `a new day keeps the chosen scope`() {
        Store.setScope("youtube", Scope.APP)
        Store.addUsage("youtube", 120)
        today = today.plusDays(1)
        open()
        assertEquals(Scope.APP, yt.scope)
        assertEquals(0, yt.usedSec)
    }

    @Test
    fun `an unknown stored scope falls back to short videos`() {
        prefs.edit().putString("scope_youtube", "SOMETHING_ELSE").apply()
        open()
        assertEquals(Scope.SHORTS, yt.scope)
    }

    @Test
    fun `usage in whole-app scope uses the same limit and extension rules`() {
        Store.setScope("youtube", Scope.APP)
        Store.setLimit("youtube", 5)
        Store.addUsage("youtube", 5 * 60)
        assertTrue(yt.exceeded)
        assertTrue(Store.extend("youtube"))
        assertFalse(yt.exceeded)
    }

}
