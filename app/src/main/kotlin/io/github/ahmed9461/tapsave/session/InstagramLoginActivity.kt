package io.github.ahmed9461.tapsave.session

import io.github.ahmed9461.tapsave.R
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import java.net.URI
import java.io.ByteArrayInputStream

class InstagramLoginActivity : Activity() {
    private lateinit var web: WebView
    @SuppressLint("SetJavaScriptEnabled") // Instagram's real sign-in page requires JS; no JS bridge or field scraping.
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val status = TextView(this).apply { text = getString(R.string.login_instructions); setPadding(16, 16, 16, 16) }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = getString(R.string.use_session)
            setOnClickListener {
                val cookie = CookieManager.getInstance().getCookie(InstagramSession.ORIGIN).orEmpty()
                if (cookie.split(';').any { it.trim().startsWith("sessionid=") }) {
                    CookieManager.getInstance().flush()
                    setResult(RESULT_OK); finish()
                } else status.text = getString(R.string.login_incomplete)
            }
        })
        layout.addView(Button(this).apply { text = getString(R.string.close); setOnClickListener { finish() } })
        web = try { WebView(this) } catch (_: RuntimeException) { status.text = getString(R.string.webview_unavailable); setContentView(layout); return }
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
