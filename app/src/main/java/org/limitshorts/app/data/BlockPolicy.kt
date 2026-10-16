package org.limitshorts.app.data

enum class BlockReason {
    /** 休息時段內。不能延長。 */
    QUIET,

    /** 今天的時間用完了。可以延長。 */
    LIMIT,
}

object BlockPolicy {
    /** 現在要不要擋?休息時段優先於時間上限。回傳 null 代表不用擋。 */
    fun decide(platform: Store.PlatformState?, quiet: QuietHours, nowMin: Int): BlockReason? = when {
        quiet.isActive(nowMin) -> BlockReason.QUIET
        platform?.exceeded == true -> BlockReason.LIMIT
        else -> null
    }
}
