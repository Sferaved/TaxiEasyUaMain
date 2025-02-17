package com.taxi.easy.ua.androidx.startup;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.anrwatchdog.ANRWatchDog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.helpers.TelegramUtils;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;
import com.taxi.easy.ua.utils.time_ut.IdleTimeoutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    private boolean isAppInForeground = false;
    private final String TAG = "MyApplication";
    private static final String LOG_FILE_NAME = "app_log.txt";
    private static MyApplication instance;
    private static Activity currentActivity = null;

    public static SharedPreferencesHelper sharedPreferencesHelperMain;

    private ThreadPoolExecutor threadPoolExecutor;
    private IdleTimeoutManager idleTimeoutManager;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferencesHelperMain = new SharedPreferencesHelper(this);
        instance = this;

        // Установка глобального обработчика исключений
        Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler(this));

        initializeFirebaseAndCrashlytics();
        setupANRWatchDog();
        setDefaultOrientation();
        registerActivityLifecycleCallbacks();
        initializeThreadPoolExecutor();
    }

    private void initializeThreadPoolExecutor() {
        // Настройка ThreadPoolExecutor
        threadPoolExecutor = new ThreadPoolExecutor(
                4,  // минимальное количество потоков
                8,  // максимальное количество потоков
                1, TimeUnit.MINUTES, // время ожидания новых задач
                new LinkedBlockingQueue<>() // очередь для задач
        );
    }

    private void setDefaultOrientation() {
        // Установка ориентации экрана в портретный режим
        // Это может не сработать для всех активити
        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    // Для получения текущей активити (необходимый метод, чтобы использовать его в setDefaultOrientation)
    private Activity getCurrentActivity() {
        return currentActivity;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    private void initializeFirebaseAndCrashlytics() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Set up Firebase Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    }

    private void setupANRWatchDog() {
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler());

        // Configure ANRWatchDog for ANR detection
        new ANRWatchDog().setANRListener(error -> {
            // Use Handler to show Toast on the main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(), R.string.anr_message, Toast.LENGTH_LONG).show();
            });
            // Log the error
            Log.d(TAG, "ANR occurred: " + error.toString());

            // Log the ANR event to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().recordException(error);
        }).start();
    }

    private void registerActivityLifecycleCallbacks() {
        // Register ActivityLifecycleCallbacks to track foreground/background state
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                currentActivity = activity;
                idleTimeoutManager = new IdleTimeoutManager(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                isAppInForeground = true;
                if (idleTimeoutManager != null) {
                    idleTimeoutManager.resetTimer();
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                isAppInForeground = false;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                }
            }
        });
    }

    public boolean isAppInForeground() {
        return isAppInForeground;
    }

    // Новый обработчик необработанных исключений для записи логов и Firebase Crashlytics
    private static class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
            // Логирование исключений
            Log.e("MyExceptionHandler", "Uncaught Exception occurred: " + throwable.getMessage(), throwable);

            // Запись ошибки в Firebase Crashlytics
            FirebaseCrashlytics.getInstance().recordException(throwable);

            // Возможная перезагрузка или очистка данных
        }
    }

    private class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        public MyUncaughtExceptionHandler(MyApplication myApplication) {
        }

        @Override
        public void uncaughtException(Thread t, @NonNull Throwable e) {
            // Запись лога
            writeLog(Log.getStackTraceString(e));

            // Сообщение об ошибке
            String errorMessage = "Uncaught exception in thread " + t.getName() + ": " + e.getMessage();

            // Отправка ошибки в Telegram

            String logFilePath = getExternalFilesDir(null) + "/app_log.txt"; // Путь к лог-файлу
            TelegramUtils.sendErrorToTelegram(errorMessage, logFilePath);
            // Перезапуск приложения или завершение работы
            System.exit(1); // Завершаем приложение
        }
    }


    public void writeLog(String log) {
        if (isExternalStorageWritable()) {
            File logFile = new File(getExternalFilesDir(null), LOG_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(logFile, true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos)) {

                // Установка украинского времени
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));

                osw.write(sdf.format(new Date()) + " - " + log);
                osw.write("\n");

                Log.d(TAG, "Log written to " + logFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e("MyAppLogger", "Failed to write log", e);
            }
        } else {
            Log.e("MyAppLogger", "External storage is not writable");
        }
    }

    // Метод для проверки доступности внешнего хранилища
    private boolean isExternalStorageWritable() {
        String state = android.os.Environment.getExternalStorageState();
        return android.os.Environment.MEDIA_MOUNTED.equals(state);
    }

    // Пример использования ThreadPoolExecutor для асинхронных задач
    public void executeBackgroundTask(Runnable task) {
        if (threadPoolExecutor != null) {
            threadPoolExecutor.execute(task);
        }
    }
}
