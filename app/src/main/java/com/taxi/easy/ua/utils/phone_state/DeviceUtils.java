package com.taxi.easy.ua.utils.phone_state;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class DeviceUtils {

    public static String getDeviceId(Context context) {
        // Получение Android ID устройства
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceSerialNumber() {
        return Build.getSerial();
    }
}

