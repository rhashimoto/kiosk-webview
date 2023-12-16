package com.shoestringresearch.twa

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DeviceOwnerReceiver : DeviceAdminReceiver() {

    @Override
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        Log.i("DeviceOwnerReceiver", "onProfileProvisioningComplete")

        val activityIntent = Intent(context, LauncherActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(activityIntent)
    }
}
