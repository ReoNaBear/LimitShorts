package org.limitshorts.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * 今天的使用量、每個平台的上限、延長次數、黑名單關鍵字、休息時段、介面模式。
 * 只存在這支手機的 SharedPreferences,服務與畫面在同一個行程內共用。
 */
object Store {
    const val EXT_MINUTES = 5
    const val MAX_EXTENSIONS = 3
    const val DEFAULT_LIMIT = 30
    const val MIN_LIMIT = 5
    const val MAX_LIMIT = 180
    const val LIMIT_STEP = 5
    const val HISTORY_DAYS = 7

    data class PlatformState(
        val id: String,
        val usedSec: Int,
        val limitMin: Int,
        val extUsed: Int,
        val scope: Scope = Scope.SHORTS,
    ) {
        val allowedSec: Int get() = (limitMin + extUsed * EXT_MINUTES) * 60
        val exceeded: Boolean get() = usedSec >= allowedSec
        val extLeft: Int get() = MAX_EXTENSIONS - extUsed
    }

    data class DayTotal(val date: LocalDate, val seconds: Int)

    data class Snapshot(
        val platforms: Map<String, PlatformState> = emptyMap(),
        val keywords: List<String> = emptyList(),
        val skipped: Int = 0,
        val quiet: QuietHours = QuietHours(),
        val simpleMode: Boolean = false,
        val onboarded: Boolean = false,
        /** 最近 7 天(含今天,最後一個是今天)每天所有平台加總的秒數。 */
        val week: List<DayTotal> = emptyList(),
    )

    private var prefsRef: SharedPreferences? = null
    private val prefs: SharedPreferences get() = checkNotNull(prefsRef) { "Store.init was not called" }
    private var ids: List<String> = emptyList()
    private var clock: () -> LocalDate = { LocalDate.now() }
    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun init(context: Context) {
        if (prefsRef != null) return
        setup(
            context.applicationContext.getSharedPreferences("store", Context.MODE_PRIVATE),
            Rules.all(context).filter { it.supported }.map { it.id },
        )
    }

    /** 測試用:換掉儲存空間、平台清單與「今天」。 */
    internal fun setup(
        prefs: SharedPreferences,
        platformIds: List<String>,
        today: () -> LocalDate = { LocalDate.now() },
    ) {
        prefsRef = prefs
        ids = platformIds
        clock = today
        rollDayIfNeeded()
        publish()
    }

    fun addUsage(id: String, seconds: Int) {
        rollDayIfNeeded()
        val dayKey = "day_${clock()}"
        prefs.edit()
            .putInt("used_$id", prefs.getInt("used_$id", 0) + seconds)
            .putInt(dayKey, prefs.getInt(dayKey, 0) + seconds)
            .apply()
        publish()
    }

    /** 用掉一次延長機會。成功回傳 true;今天的次數用完了回傳 false。 */
    fun extend(id: String): Boolean {
        rollDayIfNeeded()
        val used = prefs.getInt("ext_$id", 0)
        if (used >= MAX_EXTENSIONS) return false
        prefs.edit().putInt("ext_$id", used + 1).apply()
        publish()
        return true
    }

    fun setScope(id: String, scope: Scope) {
        prefs.edit().putString("scope_$id", scope.name).apply()
        publish()
    }

    fun setLimit(id: String, minutes: Int) {
        prefs.edit().putInt("limit_$id", minutes.coerceIn(MIN_LIMIT, MAX_LIMIT)).apply()
        publish()
    }

    fun addKeyword(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        val list = readKeywords()
        if (list.any { it.equals(w, ignoreCase = true) }) return
        writeKeywords(list + w)
    }

    fun removeKeyword(word: String) = writeKeywords(readKeywords().filterNot { it == word })

    fun addSkipped() {
        rollDayIfNeeded()
        prefs.edit().putInt("skipped", prefs.getInt("skipped", 0) + 1).apply()
        publish()
    }

    fun setQuietEnabled(value: Boolean) = putBoolean("quiet_enabled", value)
    fun setQuietStart(minutes: Int) = putInt("quiet_start", QuietHours.wrap(minutes))
    fun setQuietEnd(minutes: Int) = putInt("quiet_end", QuietHours.wrap(minutes))
    fun setSimpleMode(value: Boolean) = putBoolean("simple_mode", value)
    fun setOnboarded(value: Boolean) = putBoolean("onboarded", value)

    private fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        publish()
    }

    private fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
        publish()
    }

    private fun readKeywords(): List<String> =
        prefs.getString("keywords", "").orEmpty().split('\n').filter { it.isNotBlank() }

    private fun writeKeywords(list: List<String>) {
        prefs.edit().putString("keywords", list.joinToString("\n")).apply()
        publish()
    }

    private fun rollDayIfNeeded() {
        val today = clock().toString()
        if (prefs.getString("day", "") == today) return
        val edit = prefs.edit()
        edit.putString("day", today)
        edit.putInt("skipped", 0)
        ids.forEach { edit.remove("used_$it"); edit.remove("ext_$it") }
        // 只留最近 7 天的歷史
        val oldest = clock().minusDays(HISTORY_DAYS - 1L)
        prefs.all.keys.filter { it.startsWith("day_") }.forEach { key ->
            val date = runCatching { LocalDate.parse(key.removePrefix("day_")) }.getOrNull()
            if (date != null && date.isBefore(oldest)) edit.remove(key)
        }
        edit.apply()
    }

    private fun publish() {
        val today = clock()
        _state.value = Snapshot(
            platforms = ids.associateWith {
                PlatformState(
                    id = it,
                    usedSec = prefs.getInt("used_$it", 0),
                    limitMin = prefs.getInt("limit_$it", DEFAULT_LIMIT),
                    extUsed = prefs.getInt("ext_$it", 0),
                    scope = Scope.parse(prefs.getString("scope_$it", null)),
                )
            },
            keywords = readKeywords(),
            skipped = prefs.getInt("skipped", 0),
            quiet = QuietHours(
                enabled = prefs.getBoolean("quiet_enabled", false),
                startMin = prefs.getInt("quiet_start", 22 * 60),
                endMin = prefs.getInt("quiet_end", 7 * 60),
            ),
            simpleMode = prefs.getBoolean("simple_mode", false),
            onboarded = prefs.getBoolean("onboarded", false),
            week = (HISTORY_DAYS - 1 downTo 0).map {
                val date = today.minusDays(it.toLong())
                DayTotal(date, prefs.getInt("day_$date", 0))
            },
        )
    }
}
