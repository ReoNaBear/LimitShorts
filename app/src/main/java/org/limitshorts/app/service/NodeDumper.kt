package org.limitshorts.app.service

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

/** 把無障礙節點樹序列化成 JSON,作為撰寫各平台偵測規則的原始素材。 */
object NodeDumper {
    private const val MAX_DEPTH = 40
    private const val MAX_NODES = 3000

    fun dump(root: AccessibilityNodeInfo): JSONObject {
        val counter = intArrayOf(0)
        return node(root, 0, counter)
    }

    private fun node(n: AccessibilityNodeInfo, depth: Int, counter: IntArray): JSONObject {
        counter[0]++
        val o = JSONObject()
        o.put("cls", n.className?.toString())
        n.viewIdResourceName?.let { o.put("id", it) }
        n.text?.let { o.put("text", it.toString()) }
        n.contentDescription?.let { o.put("desc", it.toString()) }
        val r = Rect().also { n.getBoundsInScreen(it) }
        o.put("bounds", "${r.left},${r.top},${r.right},${r.bottom}")
        if (n.isSelected) o.put("selected", true)
        if (n.isClickable) o.put("clickable", true)
        if (n.isChecked) o.put("checked", true)
        if (!n.isVisibleToUser) o.put("hidden", true)

        if (depth < MAX_DEPTH) {
            val kids = JSONArray()
            for (i in 0 until n.childCount) {
                if (counter[0] >= MAX_NODES) break
                val child = n.getChild(i) ?: continue
                kids.put(node(child, depth + 1, counter))
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 33) child.recycle()
            }
            if (kids.length() > 0) o.put("children", kids)
        }
        return o
    }
}
