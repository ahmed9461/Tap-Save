package io.github.ahmed9461.tapsave.session

import android.app.Application
import android.app.Service
import android.content.*
import android.os.*
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.core.content.edit
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TapSaveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (getProcessName().endsWith(":instagram")) WebView.setDataDirectorySuffix("instagram_session")
        else WebView.disableWebView()
    }
}

/** The opt-in flag is in the main process. Cookie storage exists only in the isolated WebView profile. */
object InstagramSession {
    const val ORIGIN = "https://www.instagram.com/"
    fun enabled(context: Context) = context.getSharedPreferences("instagram-session", Context.MODE_PRIVATE).getBoolean("enabled", false)
    fun enable(context: Context, value: Boolean) { context.getSharedPreferences("instagram-session", Context.MODE_PRIVATE).edit { putBoolean("enabled", value) } }
    fun allowsCookie(uri: URI) = uri.scheme == "https" && uri.host == "www.instagram.com" && uri.port == -1 && uri.rawUserInfo == null
    fun cookies(context: Context): String? = if (enabled(context)) call(context, 1)?.getString("cookies") else null
    fun clear(context: Context): Boolean { enable(context, false); return call(context, 2)?.getBoolean("cleared") == true }

    private fun call(context: Context, operation: Int): Bundle? {
        check(Looper.myLooper() != Looper.getMainLooper())
        val done = CountDownLatch(1)
        var result: Bundle? = null
        val receiver = Messenger(Handler(Looper.getMainLooper()) { message -> result = message.data; done.countDown(); true })
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                try { Messenger(binder).send(Message.obtain(null, operation).apply { replyTo = receiver }) }
                catch (_: RemoteException) { done.countDown() }
            }
            override fun onServiceDisconnected(name: ComponentName?) { done.countDown() }
        }
        if (!context.bindService(Intent(context, InstagramSessionService::class.java), connection, Context.BIND_AUTO_CREATE)) return null
        return try { if (done.await(5, TimeUnit.SECONDS)) result else null } finally { context.unbindService(connection) }
    }
}

/** Private IPC: no cookie export, arbitrary URL operation, logging, or credential collection. */
@Suppress("DEPRECATION")
class InstagramSessionService : Service() {
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (message.sendingUid != applicationInfo.uid) return@Handler true
        val reply = message.replyTo ?: return@Handler true
        fun respond(bundle: Bundle) { try { reply.send(Message.obtain().apply { data = bundle }) } catch (_: RemoteException) { /* caller left */ } }
        try {
        val cookies = CookieManager.getInstance()
        when (message.what) {
            1 -> respond(Bundle().apply {
                val header = cookies.getCookie(InstagramSession.ORIGIN)
                if (header != null && header.length <= 65_536 && header.split(';').any { it.trim().startsWith("sessionid=") }) putString("cookies", header)
            })
            2 -> cookies.removeAllCookies {
                cookies.flush()
                WebStorage.getInstance().deleteAllData()
                // Cache belongs to this isolated profile, never another browser/application.
                WebView(this).apply { clearCache(true); clearHistory(); clearFormData(); destroy() }
                respond(Bundle().apply { putBoolean("cleared", !cookies.hasCookies()) })
            }
        }
        } catch (_: RuntimeException) { respond(Bundle()) } // Missing/broken WebView provider is a failed optional fallback.
        true
    })
    override fun onBind(intent: Intent?): IBinder = messenger.binder
}
