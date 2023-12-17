package com.shoestringresearch.twa

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

// Use adb to enable and disable.
//   adb shell dpm set-device-owner com.shoestringresearch.twa/.DeviceOwnerReceiver
//   adb shell dpm remove-active-admin com.shoestringresearch.twa/.DeviceOwnerReceiver

class DeviceOwnerReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.v("DeviceOwnerReceiver", "onEnabled")

        configurePolicy(context, getWho(context))
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.v("DeviceOwnerReceiver", "onDisabled")
        Toast.makeText(context, "Device admin disabled", Toast.LENGTH_SHORT).show()
    }

    companion object {
        @JvmStatic
        fun configurePolicy(context: Context, adminName: ComponentName) {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as DevicePolicyManager
            if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                Log.v("DeviceOwnerReceiver", "app is device owner")
                val pluggedInto = BatteryManager.BATTERY_PLUGGED_AC or
                        BatteryManager.BATTERY_PLUGGED_USB or
                        BatteryManager.BATTERY_PLUGGED_WIRELESS
                devicePolicyManager.setGlobalSetting(
                    adminName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN, pluggedInto.toString()
                )

//                // Automatically start the activity on device start.
//                val filter = IntentFilter(Intent.ACTION_MAIN)
//                filter.addCategory(Intent.CATEGORY_HOME)
//                filter.addCategory(Intent.CATEGORY_DEFAULT)
//                val activity = ComponentName(context, HomeActivity::class.java)
//                devicePolicyManager.addPersistentPreferredActivity(adminName, filter, activity)

                // Allow package activities to enter lock task mode.
                devicePolicyManager.setLockTaskPackages(
                    adminName,
                    arrayOf<String>(context.packageName))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    devicePolicyManager.setLockTaskFeatures(
                        adminName,
                        DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS)
                }
            }
        }
    }
}
