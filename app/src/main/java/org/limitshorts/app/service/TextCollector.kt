package org.limitshorts.app.service

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/** 在畫面節點樹上做事的小工具,都有節點數上限,避免在低階手機上拖慢。 */
object TextCollector {
    private const val MAX_NODES = 600

    /**
     * 收集畫面上看得到的文字(標題、說明、hashtag)。
     * topCut / bottomCut 是像素:完全落在 topCut 以上、或從 bottomCut 以下開始的節點會被略過(分頁列、導覽列)。
     */
    fun collect(root: AccessibilityNodeInfo, topCut: Int, bottomCut: Int): String {
        val sb = StringBuilder()
        walk(root, topCut, bottomCut, sb, intArrayOf(0), Rect())
        return sb.toString()
    }

    private fun walk(
        n: AccessibilityNodeInfo, topCut: Int, bottomCut: Int,
        sb: StringBuilder, count: IntArray, r: Rect,
    ) {
        if (count[0]++ >= MAX_NODES || !n.isVisibleToUser) return
        n.getBoundsInScreen(r)
        if (r.top >= bottomCut || r.bottom <= topCut) return
        n.text?.let { sb.append(it).append('\n') }
        n.contentDescription?.let { sb.append(it).append('\n') }
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            walk(c, topCut, bottomCut, sb, count, r)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 33) c.recycle()
        }
    }

    /** 有沒有「被選取」的節點,它的描述或文字符合 [matches]?找到第一個就停。 */
    fun hasSelectedTab(root: AccessibilityNodeInfo, matches: (CharSequence?) -> Boolean): Boolean =
        findSelected(root, matches, intArrayOf(0))

    private fun findSelected(n: AccessibilityNodeInfo, matches: (CharSequence?) -> Boolean, count: IntArray): Boolean {
        if (count[0]++ >= MAX_NODES) return false
        if (n.isSelected && n.isVisibleToUser && (matches(n.contentDescription) || matches(n.text))) return true
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            val found = findSelected(c, matches, count)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 33) c.recycle()
            if (found) return true
        }
        return false
    }
}
