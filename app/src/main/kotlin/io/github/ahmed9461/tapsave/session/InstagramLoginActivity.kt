package io.github.ahmed9461.tapsave.session

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import java.net.URI
import java.io.ByteArrayInputStream

@SuppressLint("SetTextI18n") // Temporary native sign-in chrome; page content is Instagram-owned.
class InstagramLoginActivity : Activity() {
    private lateinit var web: WebView
    @SuppressLint("SetJavaScriptEnabled") // Instagram's real sign-in page requires JS; no JS bridge or field scraping.
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val status = TextView(this).apply { text = "Instagram • https://www.instagram.com\nSign in on Instagram, then choose Use session. Tap Save does not read or store your password."; setPadding(16, 16, 16, 16) }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "Use session"
            setOnClickListener {
                val cookie = CookieManager.getInstance().getCookie(InstagramSession.ORIGIN).orEmpty()
                if (cookie.split(';').any { it.trim().startsWith("sessionid=") }) {
                    CookieManager.getInstance().flush()
                    setResult(RESULT_OK); finish()
                } else status.text = "Sign-in is not complete. Finish Instagram login, then choose Use session."
            }
        })
        layout.addView(Button(this).apply { text = "Close"; setOnClickListener { finish() } })
        web = try { WebView(this) } catch (_: RuntimeException) { status.text = "Android System WebView is unavailable."; setContentView(layout); return }
        web.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.saveFormData = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !navigationAllowed(request.url.toString())
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (resourceAllowed(request.url.toString())) return null
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
                }
            }
            setDownloadListener { _, _, _, _, _ -> /* Login session has no download/export channel. */ }
        }
        layout.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(layout)
        web.loadUrl("${InstagramSession.ORIGIN}accounts/login/")
    }
    override fun onDestroy() { if (::web.isInitialized) { web.stopLoading(); web.destroy() }; super.onDestroy() }
    companion object {
        fun navigationAllowed(url: String): Boolean = try {
            val uri = URI(url)
            uri.scheme == "https" && uri.host in setOf("instagram.com", "www.instagram.com") && uri.rawUserInfo == null && uri.port == -1
        } catch (_: Exception) { false }
        fun resourceAllowed(url: String): Boolean = try {
            val uri = URI(url)
            uri.scheme == "https" && uri.rawUserInfo == null && uri.port == -1 &&
                listOf("instagram.com", "cdninstagram.com", "fbcdn.net").any { uri.host == it || uri.host?.endsWith(".$it") == true }
        } catch (_: Exception) { false }
    }
}
