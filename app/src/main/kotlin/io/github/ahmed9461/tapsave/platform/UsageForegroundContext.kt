package io.github.ahmed9461.tapsave.platform

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/** Session-scoped experiment. Usage events give app context, never a current Reel URL. */
class UsageForegroundContext(context: Context) {
    private val manager = context.getSystemService(UsageStatsManager::class.java)
    private val state = ForegroundState()
    private var cursor = System.currentTimeMillis() - 5_000

    fun currentPackage(): String? {
        val now = System.currentTimeMillis()
        if (now < cursor) {
            state.clear()
            cursor = now
        }
        val events = manager.queryEvents(cursor, now) ?: run {
            state.clear()
            return null
        }
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> state.resumed(event.packageName)
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> state.paused(event.packageName)
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> state.clear()
            }
        }
        cursor = now
        return state.packageName
    }

    fun reset() {
        state.clear()
        // Unlock/resume events can precede the broadcast by a few milliseconds.
        cursor = System.currentTimeMillis() - 5_000
    }

    companion object {
        fun hasPermission(context: Context): Boolean =
            context.getSystemService(AppOpsManager::class.java).unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            ) == AppOpsManager.MODE_ALLOWED
    }
}
