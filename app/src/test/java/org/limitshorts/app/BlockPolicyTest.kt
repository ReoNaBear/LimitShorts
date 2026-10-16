package org.limitshorts.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.limitshorts.app.data.BlockPolicy
import org.limitshorts.app.data.BlockReason
import org.limitshorts.app.data.QuietHours
import org.limitshorts.app.data.Store

class BlockPolicyTest {
    private fun state(used: Int, limit: Int = 30, ext: Int = 0) =
        Store.PlatformState("youtube", usedSec = used, limitMin = limit, extUsed = ext)

    private val noQuiet = QuietHours(enabled = false)
    private val night = QuietHours(enabled = true, startMin = 22 * 60, endMin = 7 * 60)

    @Test
    fun `allowed seconds grow with each extension`() {
        assertEquals(30 * 60, state(0).allowedSec)
        assertEquals((30 + 5) * 60, state(0, ext = 1).allowedSec)
        assertEquals((30 + 15) * 60, state(0, ext = 3).allowedSec)
    }

    @Test
    fun `exceeded turns true exactly at the limit`() {
        assertFalse(state(30 * 60 - 1).exceeded)
        assertTrue(state(30 * 60).exceeded)
        assertTrue(state(30 * 60 + 1).exceeded)
    }

    @Test
    fun `extensions left counts down from three`() {
        assertEquals(3, state(0, ext = 0).extLeft)
        assertEquals(0, state(0, ext = 3).extLeft)
    }

    @Test
    fun `no block while under the limit and outside quiet hours`() {
        assertNull(BlockPolicy.decide(state(10), night, nowMin = 12 * 60))
    }

    @Test
    fun `limit reached blocks with LIMIT`() {
        assertEquals(BlockReason.LIMIT, BlockPolicy.decide(state(30 * 60), noQuiet, nowMin = 12 * 60))
    }

    @Test
    fun `an extension lifts the limit block`() {
        val s = state(30 * 60, ext = 1)
        assertNull(BlockPolicy.decide(s, noQuiet, nowMin = 12 * 60))
    }

    @Test
    fun `quiet hours block even when time is left`() {
        assertEquals(BlockReason.QUIET, BlockPolicy.decide(state(0), night, nowMin = 23 * 60))
    }

    @Test
    fun `quiet hours win over the limit reason`() {
        assertEquals(BlockReason.QUIET, BlockPolicy.decide(state(99999), night, nowMin = 2 * 60))
    }

    @Test
    fun `unknown platform is only blocked by quiet hours`() {
        assertNull(BlockPolicy.decide(null, night, nowMin = 12 * 60))
        assertEquals(BlockReason.QUIET, BlockPolicy.decide(null, night, nowMin = 23 * 60))
    }
}
