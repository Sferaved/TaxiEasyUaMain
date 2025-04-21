package com.taxi.easy.ua.ui.clear;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;

import com.taxi.easy.ua.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

    public static void clearAllSharedPreferences(Context context) {
        final String PREFS_VERSION_KEY = "SharedPrefsVersion";

        if (sharedPreferencesHelperMain != null) {
            Logger.d(context, TAG, "Начало очистки sharedPreferencesHelperMain");

            SharedPreferences prefs = sharedPreferencesHelperMain.getSharedPreferences();

            // Сохраняем текущую версию
            int savedVersion = prefs.getInt(PREFS_VERSION_KEY, -1);

            // Логируем все ключи перед удалением
            Map<String, ?> allPrefs = prefs.getAll();
            if (!allPrefs.isEmpty()) {
                Logger.d(context, TAG, "Удаляемые данные в SharedPreferences:");
                for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                    Logger.d(context, TAG, "Ключ: " + entry.getKey() + ", Значение: " + entry.getValue());
                }
            } else {
                Logger.d(context, TAG, "SharedPreferences пуст, нет данных для удаления");
            }

            // Очистка SharedPreferences
            prefs.edit().clear().apply();
            Logger.d(context, TAG, "SharedPreferences очищен");

            // Восстановление сохранённой версии
            if (savedVersion != -1) {
                prefs.edit().putInt(PREFS_VERSION_KEY, savedVersion).apply();
                Logger.d(context, TAG, "SharedPrefsVersion восстановлен: " + savedVersion);
            }

            // Обнуляем helper
            sharedPreferencesHelperMain = null;
            Logger.d(context, TAG, "sharedPreferencesHelperMain сброшен (null)");
        } else {
            Logger.d(context, TAG, "sharedPreferencesHelperMain уже null, очистка не требуется");
        }

        // Удаление .xml и .bak файлов
        File sharedPrefsDir = new File(context.getApplicationInfo().dataDir + "/shared_prefs");
        if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory()) {
            File[] files = sharedPrefsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith(".xml") || name.endsWith(".bak")) {
                        if (file.delete()) {
                            Logger.d(context, TAG, "Файл удален: " + name);
                        } else {
                            Logger.d(context, TAG, "Не удалось удалить файл: " + name);
                        }
                    } else {
                        Logger.d(context, TAG, "Пропущен файл (не .xml или .bak): " + name);
                    }
                }
            } else {
                Logger.d(context, TAG, "Нет файлов в директории shared_prefs");
            }
        } else {
            Logger.d(context, TAG, "Директория shared_prefs не существует или не является директорией");
        }
    }



    /**
     * Очистка данных
     * @param context Контекст приложения.
     */
    public static void clearData(Context context) {
        // Очистка данных приложения
        clearAppData(context);
    }

    public static void delApp(Context context) {


        // Запуск нового потока для перезапуска приложения после очистки
        new Handler().postDelayed(() -> {
            // Запуск экрана удаления приложения
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
            uninstallIntent.setData(Uri.parse("package:" + context.getPackageName()));
            uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(uninstallIntent);

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
