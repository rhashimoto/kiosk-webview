package com.shoestringresearch.kiosk.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager

const val RELOAD_DELAY_MILLIS = 2L * 60L * 1000L
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
        webView.settings.allowContentAccess = true
        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.webViewClient = CustomWebViewClient(mainLooper)
        setContentView(webView)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        webView.loadUrl(prefs.getString("url", "about:blank") ?: "")

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

private class CustomWebViewClient(val looper: Looper): WebViewClient() {
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
        error: WebResourceError
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

    private fun scheduleReload(webView: WebView) {
        Log.v("HomeActivity", "scheduleReload")
        if (!pending.contains(webView)) {
            pending.add(webView)
            Handler(looper).postDelayed(runnable, RELOAD_DELAY_MILLIS)
        }
    }
}
