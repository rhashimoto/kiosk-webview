/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shoestringresearch.twa;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

public class LauncherActivity extends com.google.androidbrowserhelper.trusted.LauncherActivity {
    private DevicePolicyManager devicePolicyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("LauncherActivity", "onCreate");

        // Using WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON here doesn't
        // work, probably because the Trusted Web Activity has its own window.
        // Try https://developer.mozilla.org/en-US/docs/Web/API/WakeLock
        // on the web side instead.

        ComponentName deviceAdmin = new ComponentName(this, DeviceOwnerReceiver.class);
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            Log.v("LauncherActivity", "app is device owner");
            int pluggedInto =
                BatteryManager.BATTERY_PLUGGED_AC |
                BatteryManager.BATTERY_PLUGGED_USB |
                BatteryManager.BATTERY_PLUGGED_WIRELESS;
            devicePolicyManager.setGlobalSetting(
                deviceAdmin,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                String.valueOf(pluggedInto));

            devicePolicyManager.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
        }

        // Setting an orientation crashes the app due to the transparent background on Android 8.0
        // Oreo and below. We only set the orientation on Oreo and above. This only affects the
        // splash screen and Chrome will still respect the orientation.
        // See https://github.com/GoogleChromeLabs/bubblewrap/issues/496 for details.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("LauncherActivity", "onResume");
//        if (devicePolicyManager.isLockTaskPermitted(getPackageName())) {
//            Log.v("LauncherActivity", "startLockTask");
//            startLockTask();
//        }
    }
}
