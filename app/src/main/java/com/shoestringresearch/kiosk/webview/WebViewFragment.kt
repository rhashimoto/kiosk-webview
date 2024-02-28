package com.shoestringresearch.kiosk.webview

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

const val DEFAULT_URL = "https://appassets.androidplatform.net/assets/default.html"

class WebViewFragment: Fragment(R.layout.webview_fragment) {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById<WebView>(R.id.webview)
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.userAgentString = "${webView.settings.userAgentString} Kiosk"

        val webViewClient = CustomWebViewClient(this)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            val swController = ServiceWorkerControllerCompat.getInstance()
            swController.setServiceWorkerClient(object: ServiceWorkerClientCompat() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    return webViewClient.shouldInterceptRequest(webView, request)
                }
            })
        }
        webView.webViewClient = webViewClient

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
        webView.loadUrl(prefs.getString("url", DEFAULT_URL)!!)

        // Reload when internet access is restored.
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("WebViewFragment", "network is available")
                lifecycleScope.launch {
                    webView.reload()
                }
            }
        }
        val connectivityManager = getSystemService(
            requireContext(),
            ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
}

private class CustomWebViewClient(val fragment: Fragment): WebViewClientCompat() {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(fragment.requireActivity()))
        .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(fragment.requireActivity()))
        .build()
    val origin: String?

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext())
        val url = prefs.getString("url", "about:blank")!!
        origin = "^(https://[^/]+)".toRegex().find(url)?.value
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceErrorCompat
    ) {
        super.onReceivedError(view, request, error)
        val description = if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION)) {
            error.description
        } else {
            ""
        }
        Log.e("CustomWebViewClient", "onReceivedError ${request.url} $description")
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        Log.e("CustomWebViewClient", "onReceivedHttpError ${request.url} ${errorResponse.statusCode}")
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        Log.v("CustomWebViewClient", "webview ${request.url}")
        if (request.url.toString().startsWith("https://appassets.androidplatform.net")) {
            when (request.url.path) {
                "/x/accessToken" -> runBlocking {
                    (fragment.requireActivity().application as Application)
                        .authorizationHelper
                        .getAuthState()?.accessToken
                }?.let { token ->
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(token.toByteArray()))
                        .apply {
                            responseHeaders = mapOf(
                                "Access-Control-Allow-Origin" to origin)
                        }
                }
            }
        }
        return assetLoader.shouldInterceptRequest(request.url)
    }
}
