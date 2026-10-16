package org.limitshorts.app

import android.content.SharedPreferences

/** 記憶體版的 SharedPreferences,讓 Store 可以不靠 Android 就測。 */
class FakePrefs : SharedPreferences {
    private val map = LinkedHashMap<String, Any?>()

    override fun getAll(): MutableMap<String, *> = LinkedHashMap(map)
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        (map[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private val removed = HashSet<String>()
        private var clear = false

        override fun putString(key: String, value: String?) = also { pending[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = also { pending[key] = values }
        override fun putInt(key: String, value: Int) = also { pending[key] = value }
        override fun putLong(key: String, value: Long) = also { pending[key] = value }
        override fun putFloat(key: String, value: Float) = also { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = also { pending[key] = value }
        override fun remove(key: String) = also { removed += key; pending.remove(key) }
        override fun clear() = also { clear = true }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clear) map.clear()
            removed.forEach { map.remove(it) }
            map.putAll(pending)
        }
    }
}
