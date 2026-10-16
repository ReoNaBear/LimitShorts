package org.limitshorts.app.data

import java.util.Locale

/**
 * 休息時段(例如晚上 10 點到早上 7 點)。時間用「一天中的第幾分鐘」表示,0..1439。
 * 起點含、終點不含;起點比終點晚代表跨過午夜。
 */
data class QuietHours(
    val enabled: Boolean = false,
    val startMin: Int = 22 * 60,
    val endMin: Int = 7 * 60,
) {
    fun isActive(nowMin: Int): Boolean {
        if (!enabled || startMin == endMin) return false
        return if (startMin < endMin) {
            nowMin in startMin until endMin
        } else {
            nowMin >= startMin || nowMin < endMin
        }
    }

    companion object {
        const val STEP = 30
        const val DAY = 24 * 60

        fun wrap(minutes: Int): Int = ((minutes % DAY) + DAY) % DAY

        fun format(minutes: Int): String {
            val m = wrap(minutes)
            return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)
        }
    }
}
