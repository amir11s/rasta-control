package com.rasta.control

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var cur = ""
    private val tick = object : Runnable {
        override fun run() { check(cur); h.postDelayed(this, 3000) }
    }

    override fun onServiceConnected() { h.post(tick) }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val p = e.packageName?.toString() ?: return
        if (p == packageName || p == "com.android.systemui") return
        cur = p
        check(p)
    }

    private fun check(p: String) {
        if (p.isEmpty() || !Usage.granted(this)) return
        val lim = Store.limits(this)[p] ?: return
        if (Usage.todayMs(this, p) >= lim * 60_000L) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            startActivity(Intent(this, LockActivity::class.java)
                .putExtra("pkg", p).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onInterrupt() {}
    override fun onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy() }
}
