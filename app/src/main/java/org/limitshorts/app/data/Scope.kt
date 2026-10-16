package org.limitshorts.app.data

/** 一個平台的時間要算哪些:只算短影片,還是整個 App 在前景的時間。 */
enum class Scope {
    SHORTS,
    APP,
    ;

    companion object {
        fun parse(name: String?): Scope = entries.firstOrNull { it.name == name } ?: SHORTS
    }
}
