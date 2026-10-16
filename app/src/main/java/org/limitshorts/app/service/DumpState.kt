package org.limitshorts.app.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/** 服務與介面之間共用的狀態。只存在記憶體與本機 SharedPreferences。 */
object DumpState {
    const val MAX_DUMPS = 500
    private const val PREFS = "dump"
    private const val KEY_RECORDING = "recording"

    data class Snapshot(
        val connected: Boolean = false,
        val recording: Boolean = false,
        val count: Int = 0,
        val lastPackage: String? = null,
    )

    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun dumpDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "dumps").apply { mkdirs() }

    /** 從磁碟載入持久化的設定與已擷取份數。 */
    fun load(context: Context) {
        val recording = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_RECORDING, false)
        val count = dumpDir(context).listFiles { f -> f.extension == "json" }?.size ?: 0
        _state.update { it.copy(recording = recording, count = count) }
    }

    fun setRecording(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RECORDING, value).apply()
        _state.update { it.copy(recording = value) }
    }

    fun setConnected(value: Boolean) = _state.update { it.copy(connected = value) }

    fun onDumped(pkg: String) = _state.update { it.copy(count = it.count + 1, lastPackage = pkg) }

    fun clear(context: Context) {
        dumpDir(context).listFiles()?.forEach { it.delete() }
        _state.update { it.copy(count = 0, lastPackage = null) }
    }
}
