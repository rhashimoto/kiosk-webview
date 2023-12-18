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
val CHEAT_CODE = listOf(
    KeyEvent.KEYCODE_VOLUME_DOWN,
    KeyEvent.KEYCODE_VOLUME_DOWN,
    KeyEvent.KEYCODE_VOLUME_UP,
    KeyEvent.KEYCODE_VOLUME_DOWN,
)

class HomeActivity: Activity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private val keyCodes = ArrayDeque<Int>()

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

        requestedOrientation = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            if (prefs.getString("orientation", "portrait") == "portrait") {
                ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
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
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.v("HomeActivity", "${event.keyCode}")
            handleKeyCode(event.keyCode)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleKeyCode(keyCode: Int) {
        keyCodes.addLast(keyCode)
        while (keyCodes.size > CHEAT_CODE.size) {
            keyCodes.removeFirst()
        }

        if (keyCodes == CHEAT_CODE) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val newValue = !prefs.getBoolean("lock", false)
            prefs.edit {
                putBoolean("lock", newValue)
            }

            Log.v("HomeActivity", "Lock task $newValue")
            Toast.makeText(
                applicationContext,
                "Lock task $newValue",
                Toast.LENGTH_SHORT).show()
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
