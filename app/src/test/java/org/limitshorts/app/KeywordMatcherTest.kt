package org.limitshorts.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.limitshorts.app.service.KeywordMatcher

class KeywordMatcherTest {
    @Test
    fun `short english word must match as a whole word`() {
        assertTrue(KeywordMatcher.matches("This is AI generated", "AI"))
        assertFalse(KeywordMatcher.matches("MAIN course", "AI"))
        assertFalse(KeywordMatcher.matches("said", "AI"))
    }

    @Test
    fun `matching ignores case`() {
        assertTrue(KeywordMatcher.matches("ai is here", "AI"))
        assertTrue(KeywordMatcher.matches("Hello WORLD", "world"))
    }

    @Test
    fun `hashtag counts as a boundary`() {
        assertTrue(KeywordMatcher.matches("#ai #fun", "AI"))
        assertFalse(KeywordMatcher.matches("#aixyz", "AI"))
    }

    @Test
    fun `english word next to chinese text still matches`() {
        assertTrue(KeywordMatcher.matches("這是AI生成的影片", "AI"))
    }

    @Test
    fun `chinese keyword matches by containment`() {
        assertTrue(KeywordMatcher.matches("今天聊聊人工智慧的未來", "人工智慧"))
        assertFalse(KeywordMatcher.matches("今天聊聊貓咪", "人工智慧"))
    }

    @Test
    fun `japanese keyword matches by containment`() {
        assertTrue(KeywordMatcher.matches("これはAIの動画です", "動画"))
    }

    @Test
    fun `words with symbols are matched literally`() {
        assertTrue(KeywordMatcher.matches("I love C++ a lot", "C++"))
        assertFalse(KeywordMatcher.matches("I love C a lot", "C++"))
    }

    @Test
    fun `blank keyword never matches`() {
        assertFalse(KeywordMatcher.matches("anything", ""))
        assertFalse(KeywordMatcher.matches("anything", "   "))
    }

    @Test
    fun `firstMatch returns the first hit or null`() {
        val words = listOf("貓", "AI", "dog")
        assertEquals("AI", KeywordMatcher.firstMatch("An AI and a dog", words))
        assertNull(KeywordMatcher.firstMatch("nothing here", words))
        assertNull(KeywordMatcher.firstMatch("anything", emptyList()))
    }
}
