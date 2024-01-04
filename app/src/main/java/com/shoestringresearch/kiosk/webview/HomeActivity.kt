package com.shoestringresearch.kiosk.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

// Time window to enter lock mode exit code with volume buttons.
const val CODE_TIMEOUT = 10L * 1000L

class HomeActivity: Activity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private val code = StringBuilder()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE)
            as DevicePolicyManager

        // This configuration is supposed to happen in the DeviceAdminReceiver,
        // but onEnabled() isn't called consistently when set-device-owner
        // is invoked via adb.
        val deviceAdmin = ComponentName(this, DeviceOwnerReceiver::class.java)
        DeviceOwnerReceiver.configurePolicy(this, deviceAdmin)

        val webView = WebView(this)
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.userAgentString = "${webView.settings.userAgentString} Kiosk"

        val webViewClient = CustomWebViewClient(this)
        webView.webViewClient = webViewClient
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            val swController = ServiceWorkerControllerCompat.getInstance()
            swController.setServiceWorkerClient(object: ServiceWorkerClientCompat() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    return webViewClient.shouldInterceptRequest(webView, request)
                }
            })
        }

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

        setContentView(webView)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        webView.loadUrl(prefs.getString("url", null) ?: "about:blank")
//        webView.loadUrl("https://appassets.androidplatform.net/assets/test.html")

        requestedOrientation =
            if (prefs.getString("orientation", "portrait") == "portrait") {
                ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }

        // Hide system bars.
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Prevent screen from dimming.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
    }

    override fun onResume() {
        super.onResume()

        if (devicePolicyManager.isLockTaskPermitted(packageName)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            if (prefs.getBoolean("lock", false)) {
                Log.v("HomeActivity", "startLockTask")
                startLockTask()
            } else {
                Log.v("HomeActivity", "stopLockTask")
                stopLockTask()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val token = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> 'u'
            KeyEvent.KEYCODE_VOLUME_DOWN-> 'd'
            else -> null
        }

        if (token != null) {
            code.append(token)
            Log.v("HomeActivity", "code $code")
            if (code.length == 1) {
                // When the first token is received, schedule the code test.
                Handler(mainLooper).postDelayed({
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                    if (prefs.getString("code", "") == code.toString()) {
                        prefs.edit {
                            putBoolean("lock", false)
                        }

                        Log.v("HomeActivity", "Unlocked")
                        Toast.makeText(
                            applicationContext,
                            "Unlocked",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Log.v("HomeActivity", "Invalid code $code")
                    }
                    code.setLength(0)
                }, CODE_TIMEOUT)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

private class CustomWebViewClient(val activity: Activity): WebViewClientCompat() {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(activity))
        .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(activity))
        .build()
    val origin: String?

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity.application)
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
                    (activity.application as Application)
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
