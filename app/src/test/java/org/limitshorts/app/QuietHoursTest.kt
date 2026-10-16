package org.limitshorts.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.limitshorts.app.data.QuietHours

class QuietHoursTest {
    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test
    fun `same-day window includes start and excludes end`() {
        val q = QuietHours(enabled = true, startMin = at(13), endMin = at(15))
        assertFalse(q.isActive(at(12, 59)))
        assertTrue(q.isActive(at(13)))
        assertTrue(q.isActive(at(14, 59)))
        assertFalse(q.isActive(at(15)))
    }

    @Test
    fun `overnight window covers both sides of midnight`() {
        val q = QuietHours(enabled = true, startMin = at(22), endMin = at(7))
        assertTrue(q.isActive(at(22)))
        assertTrue(q.isActive(at(23, 59)))
        assertTrue(q.isActive(0))
        assertTrue(q.isActive(at(6, 59)))
        assertFalse(q.isActive(at(7)))
        assertFalse(q.isActive(at(12)))
        assertFalse(q.isActive(at(21, 59)))
    }

    @Test
    fun `disabled window is never active`() {
        val q = QuietHours(enabled = false, startMin = at(22), endMin = at(7))
        assertFalse(q.isActive(at(23)))
    }

    @Test
    fun `equal start and end means no window`() {
        val q = QuietHours(enabled = true, startMin = at(9), endMin = at(9))
        assertFalse(q.isActive(at(9)))
        assertFalse(q.isActive(at(15)))
    }

    @Test
    fun `wrap keeps minutes inside one day`() {
        assertEquals(0, QuietHours.wrap(QuietHours.DAY))
        assertEquals(QuietHours.DAY - 30, QuietHours.wrap(-30))
        assertEquals(30, QuietHours.wrap(QuietHours.DAY + 30))
    }

    @Test
    fun `format pads hours and minutes`() {
        assertEquals("07:05", QuietHours.format(at(7, 5)))
        assertEquals("22:00", QuietHours.format(at(22)))
        assertEquals("00:00", QuietHours.format(QuietHours.DAY))
    }
}
