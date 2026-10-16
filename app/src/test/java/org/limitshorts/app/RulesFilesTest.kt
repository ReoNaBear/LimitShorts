package org.limitshorts.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.limitshorts.app.data.Rules
import java.io.File

/** 確保每一份規則 JSON 都格式正確,社群改規則時不會把它改壞。 */
class RulesFilesTest {
    private val dir = File("src/main/assets/rules")

    private fun files() = dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }

    @Test
    fun `rule files exist for the first batch of platforms`() {
        assertEquals(listOf("facebook.json", "instagram.json", "youtube.json"), files().map { it.name })
    }

    @Test
    fun `every rule parses and its id matches the file name`() {
        files().forEach { f ->
            val rule = Rules.parse(f.readText(Charsets.UTF_8))
            assertEquals(f.nameWithoutExtension, rule.id)
            assertTrue("${f.name}: package is empty", rule.pkg.isNotBlank())
            assertTrue("${f.name}: name is empty", rule.name.isNotBlank())
            assertTrue(rule.ignoreBottomFraction in 0f..0.5f)
        }
    }

    @Test
    fun `package names are unique`() {
        val pkgs = files().map { Rules.parse(it.readText(Charsets.UTF_8)).pkg }
        assertEquals(pkgs.size, pkgs.toSet().size)
    }

    @Test
    fun `youtube is verified and detects shorts`() {
        val rule = Rules.parse(File(dir, "youtube.json").readText(Charsets.UTF_8))
        assertEquals("com.google.android.youtube", rule.pkg)
        assertTrue(rule.supported)
        assertTrue("reel_recycler" in rule.shortsViewIds)
    }

    @Test
    fun `all three first-batch platforms are supported`() {
        files().forEach {
            val rule = Rules.parse(it.readText(Charsets.UTF_8))
            assertTrue("${rule.id} should have at least one detection signal", rule.supported)
        }
    }

    @Test
    fun `instagram detects reels by its clips view ids`() {
        val rule = Rules.parse(File(dir, "instagram.json").readText(Charsets.UTF_8))
        assertTrue("clips_viewer_view_pager" in rule.shortsViewIds)
        assertTrue(rule.selectedTabPrefixes.isEmpty())
    }

    @Test
    fun `facebook detects reels by the selected tab label`() {
        val rule = Rules.parse(File(dir, "facebook.json").readText(Charsets.UTF_8))
        assertTrue(rule.shortsViewIds.isEmpty())
        assertTrue(rule.isShortsTabLabel("Reels, tab 2 of 6"))
        assertFalse(rule.isShortsTabLabel("Home, tab 1 of 6"))
        // Reels 沒有底部導覽列,說明文字就在最下面,所以不能略過底部
        assertEquals(0f, rule.ignoreBottomFraction)
        assertTrue(rule.ignoreTopFraction > 0f)
    }

    @Test
    fun `facebook also detects a full screen reel opened from the feed`() {
        val rule = Rules.parse(File(dir, "facebook.json").readText(Charsets.UTF_8))
        assertTrue(rule.isFullScreenLabel("Reel by Someone"))
        assertFalse(rule.isFullScreenLabel("Reels, tab 2 of 6"))
        // 要佔滿大半個螢幕才算,動態裡直接播的 Reel 只佔一半多
        assertTrue(rule.fullScreenMinHeight > 0.6f)
        assertTrue(rule.fullScreenMinHeight < 0.88f)
    }

    @Test
    fun `ignored screen areas never cover the whole screen`() {
        files().forEach {
            val rule = Rules.parse(it.readText(Charsets.UTF_8))
            assertTrue(rule.ignoreTopFraction + rule.ignoreBottomFraction < 0.8f)
        }
    }

    @Test
    fun `every package listed in rules is allowed by the accessibility config`() {
        val config = File("src/main/res/xml/accessibility_service_config.xml").readText(Charsets.UTF_8)
        files().forEach {
            val pkg = Rules.parse(it.readText(Charsets.UTF_8)).pkg
            assertTrue("$pkg missing from accessibility_service_config.xml", config.contains(pkg))
        }
    }

    @Test
    fun `manifest does not request internet`() {
        val manifest = File("src/main/AndroidManifest.xml").readText(Charsets.UTF_8)
        // 註解裡提到 INTERNET 沒關係,但不能有真正的 uses-permission
        assertFalse(Regex("<uses-permission[^>]*INTERNET").containsMatchIn(manifest))
    }
}
