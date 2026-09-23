package com.taxi.easy.ua.utils.exit;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.ui.exit.AnrActivity;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Открывает экран сбоя в отдельном процессе, чтобы системный диалог падения его не закрыл.
 */
public final class CrashScreenLauncher {

    private static final long COOLDOWN_MS = 15_000L;
    private static long lastShownElapsedMs;

    private CrashScreenLauncher() {
    }

    public static boolean isAnrProcess() {
        return ServerOutageHelper.isAnrProcessName(currentProcessName());
    }

    public static void show() {
        if (isAnrProcess()) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        synchronized (CrashScreenLauncher.class) {
            if (now - lastShownElapsedMs < COOLDOWN_MS) {
                return;
            }
            lastShownElapsedMs = now;
        }
        Context context = applicationContext();
        if (context == null) {
            return;
        }
        Runnable launch = () -> start(context);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            launch.run();
        } else {
            new Handler(Looper.getMainLooper()).post(launch);
        }
    }

    /** Синхронный запуск из обработчика падения. Очередь главного потока в этот момент уже мертва. */
    public static void showFromCrash(Context context) {
        if (context == null || isAnrProcess()) {
            return;
        }
        start(context);
    }

    private static void start(Context context) {
        try {
            Intent intent = new Intent(context, AnrActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    private static Context applicationContext() {
        try {
            Context context = MyApplication.getContext();
            return context == null ? null : context.getApplicationContext();
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String currentProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/cmdline"))) {
            String line = reader.readLine();
            if (line == null) {
                return "";
            }
            int nul = line.indexOf('\0');
            if (nul >= 0) {
                line = line.substring(0, nul);
            }
            return line;
        } catch (Exception ignored) {
            return "";
        }
    }
}
