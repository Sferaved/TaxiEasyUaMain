package com.taxi.easy.ua.ui.clear;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.log.Logger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import java.io.File;
import java.io.IOException;

public class AppDataUtils {

    private static final String TAG = "AppDataUtils";

    /**
     * Рекурсивно удаляет содержимое директории.
     * @param context Контекст приложения.
     * @param directory Директория для удаления.
     */
    public static void deleteDirectory(Context context, File directory) {
        if (directory != null && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(context, file);
                    } else {
                        boolean isDeleted = file.delete();
                        if (!isDeleted) {
                            Logger.d(context, TAG, "Failed to delete file: " + file.getName());
                        }
                    }
                }
            }
            boolean isDirDeleted = directory.delete();
            if (!isDirDeleted) {
                Logger.d(context, TAG, "Failed to delete directory: " + directory.getName());
            }
        }
    }

    /**
     * Очистка данных приложения.
     * @param context Контекст приложения.
     */
    public static void clearAppData(Context context) {
        // Очистка SharedPreferences
        clearAllSharedPreferences(context);

        // Очистка базы данных
        try {
            for (String database : context.databaseList()) {
                context.deleteDatabase(database);
                Logger.d(context, TAG, "clear databases ");
            }
        } catch (Exception e) {
            Logger.d(context, TAG, "Failed to clear databases: " + e.getMessage());
        }

        // Очистка файлов
        File cacheDir = context.getCacheDir();
        deleteDirectory(context, cacheDir);

        // Очистка кэша
        try {
            Runtime.getRuntime().exec("pm clear " + context.getPackageName());
            Logger.d(context, TAG, "pm clear");
        } catch (IOException e) {
            Logger.d(context, TAG, "Failed to clear app cache: " + e.getMessage());
        }
    }

    private static void clearAllSharedPreferences(Context context) {
        File sharedPrefsDir = new File(context.getApplicationInfo().dataDir + "/shared_prefs");
        if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory()) {
            File[] files = sharedPrefsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".xml")) {
                        boolean isDeleted = file.delete();
                        if (!isDeleted) {
                            Logger.d(context, TAG, "Failed to delete SharedPreferences file: " + file.getName());
                        }
                    }
                    Logger.d(context, TAG, "clearAllSharedPreferences" +file.getName());
                }
            }
        }
    }

    /**
     * Очистка данных и запуск экрана удаления приложения.
     * @param context Контекст приложения.
     */
    public static void clearDataAndUninstall(Context context) {
        // Очистка данных приложения


        // Запуск нового потока для перезапуска приложения после очистки
        new Handler().postDelayed(() -> {
            // Перезапуск приложения
            restartApplication(context);

            // Запуск экрана удаления приложения
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
            uninstallIntent.setData(Uri.parse("package:" + context.getPackageName()));
            uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(uninstallIntent);
            clearAppData(context);
        }, 1000); // Задержка в 1 секунду для завершения очистки данных
    }

    private static void restartApplication(Context context) {
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        }
    }
}
