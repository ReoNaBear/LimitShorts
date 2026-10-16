package org.limitshorts.app

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.limitshorts.app.data.Rules
import org.limitshorts.app.data.Store
import org.limitshorts.app.service.DumpState
import org.limitshorts.app.service.LimitService
import org.limitshorts.app.ui.AppActions
import org.limitshorts.app.ui.AppRoot

class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 深色底配淺色系統圖示;enableEdgeToEdge 預設會依系統主題決定,這裡明確指定
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        Store.init(this)
        DumpState.load(this)
        val rules = Rules.all(this)
        val actions = AppActions(
            openAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            toggleRecording = { DumpState.setRecording(this, it) },
            clearDumps = { DumpState.clear(this) },
            setLimit = Store::setLimit,
            setScope = Store::setScope,
            addKeyword = Store::addKeyword,
            removeKeyword = Store::removeKeyword,
            setSimpleMode = Store::setSimpleMode,
            setQuietEnabled = Store::setQuietEnabled,
            setQuietStart = Store::setQuietStart,
            setQuietEnd = Store::setQuietEnd,
            finishOnboarding = { Store.setOnboarded(true) },
            openLanguageSettings = if (Build.VERSION.SDK_INT >= 33) {
                {
                    startActivity(
                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:$packageName")),
                    )
                }
            } else null,
        )
        setContent {
            val store by Store.state.collectAsStateWithLifecycle()
            val dump by DumpState.state.collectAsStateWithLifecycle()
            AppRoot(
                rules = rules,
                store = store,
                dump = dump,
                serviceEnabled = serviceEnabled || dump.connected,
                actions = actions,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isServiceEnabled()
        DumpState.load(this)
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val component = ComponentName(this, LimitService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == component }
    }
}
