package org.limitshorts.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.limitshorts.app.data.Rule

class RuleTest {
    private fun rule(ids: List<String> = emptyList(), prefixes: List<String> = emptyList()) =
        Rule("x", "X", "pkg.x", ids, prefixes, 0f, 0.12f)

    @Test
    fun `a rule with no signals is not supported`() {
        assertFalse(rule().supported)
    }

    @Test
    fun `view ids alone make a rule supported`() {
        assertTrue(rule(ids = listOf("a")).supported)
    }

    @Test
    fun `tab prefixes alone make a rule supported`() {
        assertTrue(rule(prefixes = listOf("Reels")).supported)
    }

    @Test
    fun `tab label matching is a case-insensitive prefix match`() {
        val r = rule(prefixes = listOf("Reels"))
        assertTrue(r.isShortsTabLabel("Reels, tab 2 of 6"))
        assertTrue(r.isShortsTabLabel("reels"))
        assertTrue(r.isShortsTabLabel("  Reels, tab 2 of 6, 1 new "))
        assertFalse(r.isShortsTabLabel("Home, tab 1 of 6"))
        assertFalse(r.isShortsTabLabel("Watch Reels"))
    }

    @Test
    fun `null or blank labels never match`() {
        val r = rule(prefixes = listOf("Reels"))
        assertFalse(r.isShortsTabLabel(null))
        assertFalse(r.isShortsTabLabel(""))
        assertFalse(r.isShortsTabLabel("   "))
    }

    @Test
    fun `full screen prefixes alone make a rule supported`() {
        assertTrue(Rule("x", "X", "pkg.x", emptyList(), emptyList(), 0f, 0f, fullScreenPrefixes = listOf("Reel")).supported)
    }

    @Test
    fun `full screen label is a prefix not followed by a letter`() {
        val r = Rule("x", "X", "pkg.x", emptyList(), emptyList(), 0f, 0f, fullScreenPrefixes = listOf("Reel"))
        assertTrue(r.isFullScreenLabel("Reel by Someone"))
        assertTrue(r.isFullScreenLabel("reel 影片"))
        assertTrue(r.isFullScreenLabel("Reel"))
        assertFalse(r.isFullScreenLabel("Reels, tab 2 of 6"))
        assertFalse(r.isFullScreenLabel("Real"))
        assertFalse(r.isFullScreenLabel(null))
        assertFalse(r.isFullScreenLabel(""))
    }

    @Test
    fun `several prefixes are all tried`() {
        val r = rule(prefixes = listOf("Reels", "短影片"))
        assertTrue(r.isShortsTabLabel("短影片，第 2 個分頁"))
    }
}
