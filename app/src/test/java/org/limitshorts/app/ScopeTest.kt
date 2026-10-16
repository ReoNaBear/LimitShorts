package org.limitshorts.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.limitshorts.app.data.Scope

class ScopeTest {
    @Test
    fun `parse reads the stored name`() {
        assertEquals(Scope.APP, Scope.parse("APP"))
        assertEquals(Scope.SHORTS, Scope.parse("SHORTS"))
    }

    @Test
    fun `parse falls back to short videos for anything else`() {
        assertEquals(Scope.SHORTS, Scope.parse(null))
        assertEquals(Scope.SHORTS, Scope.parse(""))
        assertEquals(Scope.SHORTS, Scope.parse("app"))
        assertEquals(Scope.SHORTS, Scope.parse("garbage"))
    }
}
