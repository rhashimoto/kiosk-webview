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
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.preference.PreferenceManager
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask

// Time window to enter lock mode exit code with volume buttons.
const val CODE_TIMEOUT = 10L * 1000L

val TOOTHBRUSH_WINDOW = 900000 // 2 * 60 * 60 * 1000L
val TOOTHBRUSH_TIMES = arrayOf(
    arrayOf(7, 0),
    arrayOf(8, 0),
    arrayOf(9, 0),
    arrayOf(10, 0),
    arrayOf(10, 30),
    arrayOf(11, 0),
    arrayOf(11, 30),
    arrayOf(12, 0),
    arrayOf(12, 30),
    arrayOf(13, 30),
    arrayOf(14, 30),
    arrayOf(15, 30),
    arrayOf(16, 30),
    arrayOf(17, 30),
    arrayOf(18, 30),
    arrayOf(19, 30),
)

class HomeActivity: AppCompatActivity(R.layout.home_activity) {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private val code = StringBuilder()

    enum class Screen { WEBVIEW, TOOTHBRUSH }
    private lateinit var screen: Screen

    data class Brushing(val time: Long, val duration: Int)
    private var brushing = Brushing(System.currentTimeMillis(), 0)

    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.scanRecord?.let { scanRecord ->
                val bytes = scanRecord.getManufacturerSpecificData(220)
                if (bytes != null && (bytes[3].toInt() and 1) != 0) {
                    // Brushing in progress.
                    val secs = (bytes[5].toUInt() * 256u + bytes[6].toUInt()).toInt()
                    Log.d("MainActivity", "rssi ${result.rssi} $secs s")

                    brushing = Brushing(System.currentTimeMillis(), secs)
                    setScreen(Screen.WEBVIEW)
                }
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
            setScreen(Screen.WEBVIEW)
        }

        checkToothbrush()

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

                    // Trigger brushing screen for demonstration.
                    if (code.toString() == "dd") {
                        brushing = Brushing(System.currentTimeMillis() - 8 * 3600 * 1000, 120)
                        checkToothbrush(false)
                    }
                    code.setLength(0)
                }, CODE_TIMEOUT)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun getBrushingTime() : Long {
        return brushing.time
    }

    fun getBrushingDuration() : Int {
        return brushing.duration
    }

    private fun setScreen(newScreen: Screen) {
        if (!this::screen.isInitialized || newScreen != screen) {
            screen = newScreen
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                when(newScreen) {
                    Screen.WEBVIEW -> {
                        replace<WebViewFragment>(R.id.home_fragment_container)
                    }
                    Screen.TOOTHBRUSH -> {
                        replace<ToothbrushFragment>(R.id.home_fragment_container)
                    }
                }
            }
        }
    }

    private fun checkToothbrush(reschedule: Boolean = true) {
        Log.d("HomeActivity", "checkToothbrush")
        if (System.currentTimeMillis() - brushing.time > TOOTHBRUSH_WINDOW) {
            setScreen(Screen.TOOTHBRUSH)
        }

        if (reschedule) {
            // Compute the next check time.
            val now = Calendar.getInstance()
            val next = TOOTHBRUSH_TIMES.map { time ->
                if (now.get(Calendar.HOUR_OF_DAY) < time[0] ||
                    (now.get(Calendar.HOUR_OF_DAY) == time[0] && now.get(Calendar.MINUTE) < time[1])
                ) {
                    // Today
                    (now.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, time[0])
                        set(Calendar.MINUTE, time[1])
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else {
                    // Tomorrow
                    (now.clone() as Calendar).apply {
                        add(Calendar.DATE, 1)
                        set(Calendar.HOUR_OF_DAY, time[0])
                        set(Calendar.MINUTE, time[1])
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
            }.sorted()[0]

            Log.d("HomeActivity", "scheduling ${next}, ${next - now.timeInMillis}")
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    checkToothbrush()
                }
            }, next - now.timeInMillis)
        }
    }
}
