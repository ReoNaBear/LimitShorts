package org.limitshorts.app.data

import android.content.Context
import org.json.JSONObject

/**
 * 一個平台的偵測規則,從 assets 資料夾 rules 底下的 JSON 檔載入。
 * 平台改版時,社群只要改 JSON,不用動程式。
 *
 * 判斷「正在看短影片」有兩種線索,任何一種成立就算:
 *  - shortsViewIds:短影片播放頁才有的元件 ID(YouTube、Instagram 這樣有取名字的 App)
 *  - selectedTabPrefixes:被選取的分頁,描述文字以這些字開頭(Facebook 幾乎沒有 ID,但分頁有名字)
 *
 * 兩種都空 = 這個平台還沒實測過,介面上會顯示「準備中」。
 */
data class Rule(
    val id: String,
    val name: String,
    val pkg: String,
    val shortsViewIds: List<String>,
    val selectedTabPrefixes: List<String>,
    /** 讀關鍵字時略過畫面最上面這個比例(例如分頁列、標題列)。 */
    val ignoreTopFraction: Float,
    /** 讀關鍵字時略過畫面最下面這個比例(例如底部導覽列)。 */
    val ignoreBottomFraction: Float,
    /** 要讓影片暫停時,輕點畫面高度的這個比例處(0~1)。預設在中央略偏上。 */
    val pauseTapY: Float = 0.45f,
) {
    val supported: Boolean get() = shortsViewIds.isNotEmpty() || selectedTabPrefixes.isNotEmpty()

    /** 這段文字是不是「被選取的短影片分頁」的標籤?不分大小寫。 */
    fun isShortsTabLabel(label: CharSequence?): Boolean {
        val text = label?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return false
        return selectedTabPrefixes.any { text.startsWith(it, ignoreCase = true) }
    }
}

object Rules {
    private val FILES = listOf("youtube", "instagram", "facebook")
    private var cache: List<Rule>? = null

    fun all(context: Context): List<Rule> =
        cache ?: load(context).also { cache = it }

    fun byPackage(pkg: String): Rule? = cache?.firstOrNull { it.pkg == pkg }

    /** 解析單一規則檔。格式不對會丟例外,由呼叫端決定要不要略過。 */
    fun parse(json: String): Rule {
        val o = JSONObject(json)
        return Rule(
            id = o.getString("id"),
            name = o.getString("name"),
            pkg = o.getString("package"),
            shortsViewIds = strings(o, "shortsViewIds"),
            selectedTabPrefixes = strings(o, "selectedTabPrefixes"),
            ignoreTopFraction = o.optDouble("ignoreTopFraction", 0.0).toFloat(),
            ignoreBottomFraction = o.optDouble("ignoreBottomFraction", 0.12).toFloat(),
            pauseTapY = o.optDouble("pauseTapY", 0.45).toFloat(),
        )
    }

    private fun strings(o: JSONObject, key: String): List<String> {
        val a = o.optJSONArray(key) ?: return emptyList()
        return List(a.length()) { a.getString(it) }
    }

    private fun load(context: Context): List<Rule> = FILES.mapNotNull { name ->
        runCatching {
            val text = context.assets.open("rules/$name.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            parse(text)
        }.getOrNull()
    }
}
