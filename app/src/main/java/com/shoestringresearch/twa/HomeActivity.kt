package com.shoestringresearch.twa

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

const val RELOAD_DELAY_MILLIS = 2L * 60L * 1000L

class HomeActivity: Activity() {
    private lateinit var devicePolicyManager: DevicePolicyManager

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

        webView.loadUrl("https://rhashimoto.github.io/twa-iframe/web/dist/")

        requestedOrientation = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
            Log.v("LauncherActivity", "startLockTask")
            startLockTask()
        }
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
        Log.e("HomeActivity", "onReceivedError ${request.url} ${error.description}")
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
