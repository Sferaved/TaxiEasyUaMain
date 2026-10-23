package com.taxi.easy.ua.utils.download;

import android.os.Build;

/**
 * Не открывать MainActivity из {@code MY_PACKAGE_REPLACED} и {@code BOOT_COMPLETED}
 * на Android 10+. Система блокирует старт Activity из фона и оставляет пустую задачу:
 * первое нажатие на иконку после обновления ничего не показывает, второе уже открывает приложение.
 * Немедленное обновление из Play само возвращает пользователя в приложение.
 */
public final class AppUpdateLaunchPolicy {

    private AppUpdateLaunchPolicy() {
    }

    public static boolean shouldAutoStartMainActivity(int sdkInt) {
        return sdkInt < Build.VERSION_CODES.Q;
    }
}
