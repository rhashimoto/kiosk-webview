package com.shoestringresearch.twa

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView


class HomeActivity: Activity() {
    private lateinit var devicePolicyManager: DevicePolicyManager


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
        setContentView(webView)

        webView.loadUrl("https://rhashimoto.github.io/twa-iframe/web/dist/")

        requestedOrientation = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onResume() {
        super.onResume()

        if (devicePolicyManager.isLockTaskPermitted(packageName)) {
            Log.v("LauncherActivity", "startLockTask")
            startLockTask()
        }

//        // Open the TWA.
//        val intent = Intent(this, LauncherActivity::class.java)
//        startActivity(intent)
    }
}
