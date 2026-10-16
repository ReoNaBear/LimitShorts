package org.limitshorts.app.service

/** 純邏輯,不依賴 Android,可以直接用單元測試驗證。 */
object KeywordMatcher {
    private val cache = HashMap<String, Regex>()

    fun firstMatch(text: String, words: List<String>): String? = words.firstOrNull { matches(text, it) }

    /** 純英數的字(例如 AI)要整個字才算,避免 "AI" 誤中 "MAIN";中文等其他字則只要包含就算。 */
    fun matches(text: String, word: String): Boolean {
        val w = word.trim()
        if (w.isEmpty()) return false
        val asciiWord = w.all { it.code < 128 && it.isLetterOrDigit() }
        if (!asciiWord) return text.contains(w, ignoreCase = true)
        val re = cache.getOrPut(w) {
            Regex("(?<![A-Za-z0-9])${Regex.escape(w)}(?![A-Za-z0-9])", RegexOption.IGNORE_CASE)
        }
        return re.containsMatchIn(text)
    }
}
