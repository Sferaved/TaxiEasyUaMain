package com.taxi.easy.ua.utils.activ_push;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver", "onReceive called");
        Log.d("BootReceiver", "Action: " + intent.getAction());
        try {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.d("BootReceiver", "Boot completed action received");
                Intent serviceIntent = new Intent(context, MyService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        } catch (Exception e) {
            Log.e("BootReceiver", "Error starting service", e);
        }
    }

    private boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

