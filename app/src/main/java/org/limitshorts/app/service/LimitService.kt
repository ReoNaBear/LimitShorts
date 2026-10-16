package org.limitshorts.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject
import org.limitshorts.app.data.BlockPolicy
import org.limitshorts.app.data.QuietHours
import org.limitshorts.app.data.Rule
import org.limitshorts.app.data.Rules
import org.limitshorts.app.data.Scope
import org.limitshorts.app.data.Store
import java.io.File
import java.time.LocalTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 核心服務:
 *  1. 判斷現在是不是在看短影片(依 assets/rules 的規則)
 *  2. 在短影片裡每秒累計該平台今天的時間,超過上限就用 [BlockOverlay] 擋住
 *  3. 畫面上出現黑名單關鍵字就自動往上滑到下一支
 *  4. 開發者診斷:把節點樹寫成 JSON(DumpState.recording)
 */
class LimitService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private lateinit var overlay: BlockOverlay

    private var current: Rule? = null
    private var ticking = false
    private var lastScanAt = 0L
    private var skipUntil = 0L

    private val blockToken = Any()
    private var blockStarting = false
    private var stubbornTicks = 0
    private var lastEnforceAt = 0L
    private var restarts = 0
    private var lastDumpAt = 0L
    private var lastTreeHash = 0
    private val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US)

    private val tick = Runnable { onTick() }

    override fun onServiceConnected() {
        Store.init(this)
        overlay = BlockOverlay(this)
        DumpState.load(this)
        DumpState.setConnected(true)
        handler.postDelayed({ overlay.warmUp() }, 800)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val rule = Rules.byPackage(pkg) ?: return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        record(root, pkg)

        if (inScope(root, rule)) enterScope(rule, root) else leaveScope()
    }

    /**
     * 現在算不算「在計時範圍內」?
     *  - 範圍是「整個 App」:只要這個 App 在前景就算
     *  - 範圍是「只算短影片」:要在短影片頁才算
     */
    private fun inScope(root: AccessibilityNodeInfo, rule: Rule): Boolean {
        val scope = Store.state.value.platforms[rule.id]?.scope ?: Scope.SHORTS
        return scope == Scope.APP || (rule.supported && isShorts(root, rule))
    }

    // ---- 偵測與計時 ----

    /** 規則裡任何一種線索成立,就算正在看短影片。 */
    private fun isShorts(root: AccessibilityNodeInfo, rule: Rule): Boolean =
        hasShortsViewId(root, rule) ||
            (rule.selectedTabPrefixes.isNotEmpty() && TextCollector.hasSelectedTab(root, rule::isShortsTabLabel)) ||
            (rule.fullScreenPrefixes.isNotEmpty() &&
                TextCollector.hasLargeLabeledNode(
                    root,
                    (resources.displayMetrics.heightPixels * rule.fullScreenMinHeight).toInt(),
                    rule::isFullScreenLabel,
                ))

    private fun hasShortsViewId(root: AccessibilityNodeInfo, rule: Rule): Boolean =
        rule.shortsViewIds.any { id ->
            val nodes = root.findAccessibilityNodeInfosByViewId("${rule.pkg}:id/$id")
            val visible = nodes.any { it.isVisibleToUser }
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 33) nodes.forEach { it.recycle() }
            visible
        }

    private fun enterScope(rule: Rule, root: AccessibilityNodeInfo) {
        current = rule
        if (!ticking) {
            ticking = true
            handler.postDelayed(tick, 1000)
        }
        checkLimit(rule)
        // 自動滑走只在真的短影片頁做;在一般動態裡往上滑只會把動態捲走
        if (!overlay.isShowing && rule.supported && isShorts(root, rule)) scanKeywords(rule, root)
    }

    private fun leaveScope() {
        resetStubborn()
        current = null
        ticking = false
        handler.removeCallbacks(tick)
        cancelBlockStart()
        overlay.hide()
        overlay.releaseAudioFocus()
    }

    private fun onTick() {
        val rule = current ?: return
        val root = rootInActiveWindow
        // 目前的視窗是我們自己的覆蓋畫面:代表正在擋,還沒離開,繼續等(不計時)
        if (overlay.isShowing && root?.packageName?.toString() == packageName) {
            handler.postDelayed(tick, 1000)
            return
        }
        // 回到桌面、換 App、關螢幕時拿不到目標 App 的畫面,就停止計時
        if (root == null || root.packageName?.toString() != rule.pkg || !inScope(root, rule)) {
            leaveScope()
            return
        }
        if (!overlay.isShowing) Store.addUsage(rule.id, 1)
        checkLimit(rule)
        handler.postDelayed(tick, 1000)
    }

    // ---- 阻擋 ----

    /**
     * 每秒檢查一次:該不該擋?休息時間優先,其次是今天的時間上限。
     * 條件消失(延長了、休息時間結束了)就把覆蓋畫面收掉。
     */
    private fun checkLimit(rule: Rule) {
        val snap = Store.state.value
        val reason = BlockPolicy.decide(snap.platforms[rule.id], snap.quiet, minuteOfDay())
        if (reason == null) {
            resetStubborn()
            cancelBlockStart()
            overlay.hide()
            return
        }
        if (overlay.isShowing) {
            enforceStopped(rule)
            return
        }
        if (!blockStarting) beginBlock(rule)
    }

    /**
     * 擋之前,先讓影片真的停下來。順序很重要:
     *  1. 搶音訊焦點 + 送暫停鍵。YouTube 這樣就會暫停;Instagram 不理焦點。
     *  2. 0.6 秒後還有媒體在播,才點一下影片中央讓它暫停(點擊是「切換」,
     *     所以只有確定還在播才點,否則會把已經暫停的影片點成播放)。覆蓋畫面會擋住點擊,所以要在它出現之前做。
     *  3. 再過 0.6 秒,才顯示覆蓋畫面。
     */
    private fun beginBlock(rule: Rule) {
        blockStarting = true
        stubbornTicks = 0
        overlay.holdAudioFocus()
        pauseMedia()
        handler.postAtTime({
            if (isMediaPlaying() && onShortsPage(rule)) tapToPause(rule)
            handler.postAtTime({ presentBlock(rule) }, blockToken, SystemClock.uptimeMillis() + TAP_SETTLE_MS)
        }, blockToken, SystemClock.uptimeMillis() + FOCUS_SETTLE_MS)
    }

    private fun presentBlock(rule: Rule) {
        blockStarting = false
        val snap = Store.state.value
        val reason = BlockPolicy.decide(snap.platforms[rule.id], snap.quiet, minuteOfDay()) ?: run {
            overlay.releaseAudioFocus()
            return
        }
        overlay.show(
            reason = reason,
            platform = rule.name,
            extensionsLeft = snap.platforms[rule.id]?.extLeft ?: 0,
            quietFrom = QuietHours.format(snap.quiet.startMin),
            quietTo = QuietHours.format(snap.quiet.endMin),
            onRest = {
                overlay.hide()
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
            onExtend = {
                if (Store.extend(rule.id)) overlay.hide()
            },
        )
    }

    private fun cancelBlockStart() {
        handler.removeCallbacksAndMessages(blockToken)
        blockStarting = false
    }

    /**
     * 覆蓋畫面都出現了,影片卻還在播(暫停鍵太晚生效、自動播下一支…)。一步一步加強:
     *  1. 先再送一次暫停
     *  2. 重新來一次「暫停、點一下、再蓋上」,最多 3 次
     *  3. 還是不行,才回桌面
     * 回桌面是最後手段:YouTube 會縮成小視窗繼續播,而且小視窗我們管不到。
     * 所以前面要給足時間,尤其是「一秒才算一次」,不能讓影片還沒來得及暫停就被送走。
     */
    private fun enforceStopped(rule: Rule) {
        // 畫面一有變動就會進來,一秒內可能很多次。這裡要真的「一秒算一次」,
        // 否則覆蓋畫面一出現、影片還沒來得及暫停,就瞬間用完次數而被送回桌面
        val now = SystemClock.uptimeMillis()
        if (now - lastEnforceAt < ENFORCE_INTERVAL_MS) return
        lastEnforceAt = now
        if (!isMediaPlaying()) {
            stubbornTicks = 0
            return
        }
        stubbornTicks++
        if (stubbornTicks == 2) pauseMedia()
        if (stubbornTicks < 4) return
        stubbornTicks = 0
        when {
            restarts < MAX_RESTARTS -> {
                restarts++
                overlay.hide()
                cancelBlockStart()
                beginBlock(rule)
            }
            else -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun resetStubborn() {
        stubbornTicks = 0
        restarts = 0
    }

    private fun onShortsPage(rule: Rule): Boolean {
        val root = rootInActiveWindow ?: return false
        return root.packageName?.toString() == rule.pkg && rule.supported && isShorts(root, rule)
    }

    /** 現在有沒有任何媒體聲音正在播放(只看「播放中」的播放器,不分哪個 App)。 */
    private fun isMediaPlaying(): Boolean = runCatching {
        getSystemService(AudioManager::class.java).activePlaybackConfigurations.any {
            it.audioAttributes.usage == AudioAttributes.USAGE_MEDIA
        }
    }.getOrDefault(false)

    /** 在畫面中央輕點一下(短影片點一下 = 暫停)。 */
    private fun tapToPause(rule: Rule) {
        val m = resources.displayMetrics
        val path = Path().apply { moveTo(m.widthPixels * 0.5f, m.heightPixels * rule.pauseTapY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun minuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    private fun pauseMedia() {
        val am = getSystemService(AudioManager::class.java)
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }

    // ---- 關鍵字自動跳過 ----

    private fun scanKeywords(rule: Rule, root: AccessibilityNodeInfo) {
        val words = Store.state.value.keywords
        if (words.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        if (now < skipUntil || now - lastScanAt < SCAN_INTERVAL_MS) return
        lastScanAt = now

        val height = resources.displayMetrics.heightPixels
        val text = TextCollector.collect(
            root,
            topCut = (height * rule.ignoreTopFraction).toInt(),
            bottomCut = (height * (1f - rule.ignoreBottomFraction)).toInt(),
        )
        if (KeywordMatcher.firstMatch(text, words) == null) return

        skipUntil = now + SKIP_COOLDOWN_MS
        Store.addSkipped()
        swipeUp()
    }

    private fun swipeUp() {
        val m = resources.displayMetrics
        val path = Path().apply {
            moveTo(m.widthPixels * 0.5f, m.heightPixels * 0.72f)
            lineTo(m.widthPixels * 0.5f, m.heightPixels * 0.28f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 260))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // ---- 開發者診斷:節點樹記錄 ----

    private fun record(root: AccessibilityNodeInfo, pkg: String) {
        val s = DumpState.state.value
        if (!s.recording || s.count >= DumpState.MAX_DUMPS) return
        val now = SystemClock.uptimeMillis()
        if (now - lastDumpAt < DUMP_INTERVAL_MS) return
        lastDumpAt = now

        val tree = NodeDumper.dump(root)
        val body = tree.toString()
        if (body.hashCode() == lastTreeHash) return
        lastTreeHash = body.hashCode()

        val doc = JSONObject()
            .put("package", pkg)
            .put("capturedAt", System.currentTimeMillis())
            .put("androidSdk", Build.VERSION.SDK_INT)
            .put("tree", tree)
            .toString(1)
        val file = File(DumpState.dumpDir(this), "${stamp.format(Date())}_$pkg.json")
        writer.execute { file.writeText(doc) }
        DumpState.onDumped(pkg)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        leaveScope()
        DumpState.setConnected(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        writer.shutdown()
        super.onDestroy()
    }

    private companion object {
        const val DUMP_INTERVAL_MS = 1500L
        const val SCAN_INTERVAL_MS = 700L
        const val SKIP_COOLDOWN_MS = 1500L
        const val FOCUS_SETTLE_MS = 600L
        const val TAP_SETTLE_MS = 600L
        const val ENFORCE_INTERVAL_MS = 900L
        const val MAX_RESTARTS = 3
    }
}
