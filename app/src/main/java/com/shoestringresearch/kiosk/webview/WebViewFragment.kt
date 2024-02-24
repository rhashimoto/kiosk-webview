package com.shoestringresearch.kiosk.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

class WebViewFragment: Fragment(R.layout.webview_fragment) {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = view.findViewById<WebView>(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.webViewClient = CustomWebViewClient(this)

        // Send JavaScript console output to logcat.
        webView.webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                val s = "${m.sourceId()}:${m.lineNumber()} ${m.message()}"
                when (m.messageLevel()) {
                    ConsoleMessage.MessageLevel.DEBUG -> Log.d("WebChromeClient", s)
                    ConsoleMessage.MessageLevel.LOG -> Log.i("WebChromeClient", s)
                    ConsoleMessage.MessageLevel.WARNING -> Log.w("WebChromeClient", s)
                    ConsoleMessage.MessageLevel.ERROR -> Log.e("WebChromeClient", s)
                    else -> Log.i("WebChromeClient", s)
                }
                return true
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        webView.loadUrl(prefs.getString("url", null) ?: "about:blank")
//        webView.loadUrl("https://appassets.androidplatform.net/assets/test.html")
    }
}

private class CustomWebViewClient(val fragment: Fragment): WebViewClientCompat() {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(fragment.requireActivity()))
        .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(fragment.requireActivity()))
        .build()

    val pending = HashSet<WebView>()
    val runnable = Runnable {
        Log.v("HomeActivity", "reloading")
        pending.forEach {
            it.reload()
        }
        pending.clear()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceErrorCompat
    ) {
        super.onReceivedError(view, request, error)
        Log.e("HomeActivity", "onReceivedError ${request.url} $error")
        scheduleReload(view)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        Log.e("HomeActivity", "onReceivedHttpError ${request.url}")
        if (!request.url.toString().endsWith("favicon.ico")) {
            scheduleReload(view)
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.url.toString().startsWith("https://appassets.androidplatform.net")) {
            if (request.url.path == "/x/token") {
                // Return OAuth2 access token.
                runBlocking {
                    (fragment.requireActivity().application as Application)
                        .authorizationHelper
                        .getAuthState()?.accessToken
                }?.let { token ->
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(token.toByteArray())
                    )
                }
            }
        }
        return assetLoader.shouldInterceptRequest(request.url)
    }

    private fun scheduleReload(webView: WebView) {
        Log.v("HomeActivity", "scheduleReload")
        if (!pending.contains(webView)) {
            pending.add(webView)
            Handler(fragment.requireActivity().mainLooper).postDelayed(runnable, RELOAD_DELAY_MILLIS)
        }
    }
}
