package org.limitshorts.app.service

import android.accessibilityservice.AccessibilityService
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.limitshorts.app.data.BlockReason
import org.limitshorts.app.ui.BlockScreen

/**
 * 用無障礙服務自己的覆蓋視窗(TYPE_ACCESSIBILITY_OVERLAY)擋住畫面,
 * 所以不需要使用者另外授權「顯示在其他 App 上層」。
 */
class BlockOverlay(private val service: AccessibilityService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var view: ComposeView? = null
    private var owner: Owner? = null
    private var warming = false
    private val audio = service.getSystemService(AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null

    /** 暖機用的隱形視窗不算「正在擋」。 */
    val isShowing: Boolean get() = view != null && !warming

    /**
     * 第一次畫出 Compose 畫面要初始化不少東西,在較慢的手機上會讓覆蓋畫面晚好幾秒才出現。
     * 服務一連上就先畫一個 1 像素、不可觸碰的隱形視窗,把這段時間先花掉。
     */
    fun warmUp() {
        if (view != null) return
        warming = true
        attach(
            WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ),
        ) { BlockScreen(BlockReason.LIMIT, "", 0, "", "", {}, {}) }
        handler.postDelayed({ if (warming) remove() }, WARM_UP_MS)
    }

    fun show(
        reason: BlockReason,
        platform: String,
        extensionsLeft: Int,
        quietFrom: String,
        quietTo: String,
        onRest: () -> Unit,
        onExtend: () -> Unit,
    ) {
        if (warming) remove()
        if (view != null) return
        attach(blockingParams()) {
            BlockScreen(reason, platform, extensionsLeft, quietFrom, quietTo, onRest, onExtend)
        }
        holdAudioFocus()
    }

    /**
     * 讓底下的 App 停止出聲。
     * 媒體暫停鍵對 YouTube Shorts 這類沒有註冊媒體工作階段的播放器無效,連回到桌面它都還在背景播,
     * 所以改成「搶音訊焦點」:Android 規定別人要播聲音時,現在播的 App 應該暫停。
     * 用「永久」焦點(GAIN)而不是「暫時」:被暫時搶走的 App 會在我們放手時自己恢復播放,
     * 也就是使用者按了「去休息」之後,影片又在背景響起來。永久搶走的 App 不會自動恢復。
     * 被搶走焦點的只有「正在出聲」的那個 App(也就是底下的短影片),不會影響本來就停著的音樂 App。
     */
    fun holdAudioFocus() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = request
        audio.requestAudioFocus(request)
    }

    fun releaseAudioFocus() {
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    /**
     * 擋住短影片,但**不蓋住狀態列和導覽列**:使用者永遠要有辦法用系統的返回、首頁鍵離開。
     * (三顆按鈕導覽列的手機上,如果蓋住導覽列,使用者就只剩我們的按鈕可按。)
     */
    private fun blockingParams(): WindowManager.LayoutParams {
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // NOT_FOCUSABLE:不要搶走焦點,否則系統會把這個覆蓋視窗當成「目前的視窗」,
            // 服務就看不到底下的 App 了(按鈕仍然可以點)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        )
        p.gravity = Gravity.TOP
        // 視窗頂端要讓開狀態列,底部要讓開導覽列:高度 = 螢幕 - 狀態列 - 導覽列
        val status = statusBarHeight()
        if (Build.VERSION.SDK_INT >= 30) p.fitInsetsTypes = WindowInsets.Type.statusBars() else p.y = status
        p.height = screenHeight() - status - navigationBarHeight()
        return p
    }

    private fun statusBarHeight(): Int {
        val fromInsets = if (Build.VERSION.SDK_INT >= 30) {
            wm.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
        } else 0
        val res = service.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        return maxOf(fromInsets, if (id > 0) res.getDimensionPixelSize(id) else 0)
    }

    private fun screenHeight(): Int =
        if (Build.VERSION.SDK_INT >= 30) wm.currentWindowMetrics.bounds.height()
        else service.resources.displayMetrics.heightPixels

    /**
     * 導覽列的高度。系統不一定會替這種視窗避開導覽列(Sharp 的三顆按鈕是由桌面程式的
     * Taskbar 視窗畫的),服務自己量到的 insets 也可能偏小,所以取下列的最大值:
     *  1. 系統回報的 insets
     *  2. 無障礙服務看到的「系統視窗」(導覽列)實際範圍
     *  3. 按鈕導覽時,Android 預設的 48dp(查不到時的保險)
     * 手勢導覽只有一條細細的手勢列,只用 1 和 2,才不會多留一條空白。
     */
    private fun navigationBarHeight(): Int {
        val fromInsets = if (Build.VERSION.SDK_INT >= 30) {
            wm.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom
        } else 0
        val measured = maxOf(fromInsets, systemWindowBarHeight())
        val gestureNav = Settings.Secure.getInt(service.contentResolver, "navigation_mode", 0) == 2
        if (gestureNav) return measured
        val fallback = (48 * service.resources.displayMetrics.density).toInt()
        return maxOf(measured, fallback)
    }

    /** 無障礙服務看得到的系統視窗裡,貼著畫面底部、又寬又扁的那個(導覽列)的高度。 */
    private fun systemWindowBarHeight(): Int {
        val h = screenHeight()
        val w = service.resources.displayMetrics.widthPixels
        val r = Rect()
        val windows = runCatching { service.windows }.getOrNull().orEmpty()
        var best = 0
        for (win in windows) {
            if (win.type != AccessibilityWindowInfo.TYPE_SYSTEM) continue
            win.getBoundsInScreen(r)
            val atBottom = r.bottom >= h - 2 && r.top > h / 2
            if (atBottom && r.width() >= w / 2) best = maxOf(best, h - r.top)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 33) win.recycle()
        }
        return best
    }

    private fun attach(params: WindowManager.LayoutParams, content: @Composable () -> Unit) {
        val lifecycleOwner = Owner()
        val cv = ComposeView(localizedContext()).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent(content)
        }
        wm.addView(cv, params)
        view = cv
        owner = lifecycleOwner
    }

    /** 無障礙服務預設跟著系統語言;使用者若在系統設定裡替本 App 指定了語言,這裡要跟著用。 */
    private fun localizedContext(): Context {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = service.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) {
                val config = Configuration(service.resources.configuration).apply { setLocales(locales) }
                return service.createConfigurationContext(config)
            }
        }
        return service
    }

    /** 收掉覆蓋畫面。暖機中的隱形視窗不理會這個呼叫,讓它把暖機做完。 */
    fun hide() {
        if (warming) return
        remove()
    }

    private fun remove() {
        releaseAudioFocus()
        val cv = view ?: return
        owner?.destroy()
        runCatching { wm.removeView(cv) }
        view = null
        owner = null
        warming = false
    }

    private class Owner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private companion object {
        const val WARM_UP_MS = 2500L
    }
}
