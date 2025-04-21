package com.taxi.easy.ua.utils.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.log.Logger;
import com.uxcam.UXCam;

import java.util.Collections;

public class AppUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "AppUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Logger.d(context, TAG, "App updated or device rebooted, starting MainActivity");
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                context.startActivity(mainIntent);
                UXCam.logEvent("AppRestart", Collections.singletonMap("success", true));
            } catch (Exception e) {
                Logger.e(context, TAG, "Failed to start MainActivity: " + e.getMessage());
                UXCam.logEvent("AppRestart", Collections.singletonMap("success", false));
            }
        }
    }
}
