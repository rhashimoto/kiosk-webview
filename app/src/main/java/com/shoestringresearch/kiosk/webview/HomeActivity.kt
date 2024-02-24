package com.shoestringresearch.kiosk.webview

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager

// Time window to enter lock mode exit code with volume buttons.
const val CODE_TIMEOUT = 10L * 1000L

class HomeActivity: AppCompatActivity(R.layout.home_activity) {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private val code = StringBuilder()

    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.scanRecord?.let { scanRecord ->
                val bytes = scanRecord.getManufacturerSpecificData(220)
                requireNotNull(bytes)
                val state = bytes[3].toInt() and 1
                val secs = bytes[5].toUInt() * 256u + bytes[6].toUInt()
                Log.d("MainActivity", "rssi ${result.rssi} on/off $state $secs s")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE)
            as DevicePolicyManager

        // This configuration is supposed to happen in the DeviceAdminReceiver,
        // but onEnabled() isn't called consistently when set-device-owner
        // is invoked via adb.
        val deviceAdmin = ComponentName(this, DeviceOwnerReceiver::class.java)
        DeviceOwnerReceiver.configurePolicy(this, deviceAdmin)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<WebViewFragment>(R.id.home_fragment_container)
//                add<ToothbrushFragment>(R.id.home_fragment_container)
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
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

        Log.d("MainActivity", "start toothbrush scan")
        val mac = prefs.getString("toothbrush_mac", "")
        if (requireNotNull(mac).isNotEmpty()) {
            val bluetoothManager: BluetoothManager =
                getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

            val filter = ScanFilter.Builder()
//                .setDeviceAddress("68:E7:4A:49:E1:A6")
                .setDeviceAddress(mac)
                .setManufacturerData(220, null)
                .build()
            val settings = ScanSettings.Builder()
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            bluetoothAdapter.bluetoothLeScanner.startScan(listOf(filter), settings, scanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        val bluetoothManager: BluetoothManager =
            getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)

        super.onDestroy()
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
