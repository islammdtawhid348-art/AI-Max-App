package com.aimax.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MaxService : AccessibilityService() {
    companion object {
        @Volatile var pending = false
        @Volatile var until = 0L
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (!pending) return
        if (System.currentTimeMillis() > until) { pending = false; return }
        val root = rootInActiveWindow ?: return
        var node: AccessibilityNodeInfo? =
            root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send").firstOrNull()
        if (node == null) {
            for (w in listOf("Send", "পাঠান")) {
                node = root.findAccessibilityNodeInfosByText(w).firstOrNull()
                if (node != null) break
            }
        }
        var n = node
        while (n != null && !n.isClickable) n = n.parent
        if (n != null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) pending = false
    }

    override fun onInterrupt() {}
}
