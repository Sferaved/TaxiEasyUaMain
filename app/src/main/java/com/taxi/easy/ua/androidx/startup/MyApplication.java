package com.taxi.easy.ua.androidx.startup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavOptions;

import com.github.anrwatchdog.ANRWatchDog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.exit.AnrActivity;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.keys.SecurePrefs;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;
import com.taxi.easy.ua.utils.time_ut.IdleTimeoutManager;
import com.uxcam.UXCam;
import com.uxcam.datamodel.UXConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;

public class MyApplication extends Application {

    private boolean isAppInForeground = false;
    private final String TAG = "MyApplication";
    private static final String LOG_FILE_NAME = "app_log.txt";
    @SuppressLint("StaticFieldLeak")
    private static MyApplication instance;
    @SuppressLint("StaticFieldLeak")
    private static Activity currentActivity = null;

    public static SharedPreferencesHelper sharedPreferencesHelperMain;

    private IdleTimeoutManager idleTimeoutManager;

    private long lastMemoryWarningTime = 0;
    private long lastInternetWarningTime = 0;
    private boolean isUXCamInitialized = false;
    private static final int MAX_RETRY_ATTEMPTS = 2; // Максимум попыток загрузки ключа
    FirestoreHelper firestoreHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
        Log.d("LocaleDebug", "Current locale: " + currentLocale.toString());

        try {

            initializeFirebaseAndCrashlytics();

            setDefaultOrientation();

            sharedPreferencesHelperMain = new SharedPreferencesHelper(this);

            firestoreHelper = new FirestoreHelper(this);
            firestoreHelper.listenForResponseChanges();

            registerActivityLifecycleCallbacks();

            setupCrashHandler();

            setupANRWatchDog();

            fetchUXCamKey(1);


            visicomKeyFromFb();
            mapboxKeyFromFb ();

        } catch (Exception e) {
            Logger.e(this, TAG, "Initialization failed: " + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        instance = this;
    }
    private void setupCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Логгируем исключение
            FirebaseCrashlytics.getInstance().recordException(throwable);

            Logger.e(getApplicationContext(), "CrashHandler", "Crash caught: " + throwable.getMessage());
            Logger.e(getApplicationContext(), "CrashHandler", "Crash caught: " + Log.getStackTraceString(throwable));

            // Сохраняем stacktrace в SharedPreferences
            sharedPreferencesHelperMain.saveValue("last_crash", Log.getStackTraceString(throwable));
            // Запускаем AnrActivity на главном потоке

            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(getApplicationContext(), AnrActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });

            // Завершаем процесс через 1 секунду, чтобы успела запуститься активность
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }, 1000);
        });
    }

    private void fetchUXCamKey(int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            Logger.e(this, TAG, "Max retry attempts reached for fetching UXCam key");
            return;
        }

        try {
            String apiKey = SecurePrefs.getKey(this);
            Context context = getApplicationContext();

            if (apiKey != null && !isUXCamInitialized) {
                Logger.d(this, TAG, "Using cached UXCam key");
                UXConfig config = new UXConfig.Builder(apiKey)
                        .enableAutomaticScreenNameTagging(true)
                        .enableImprovedScreenCapture(true)
                        .build();

                UXCam.startWithConfiguration(config);

                // Проверяем успешность инициализации через тестовое событие
                try {
                    UXCam.logEvent("TestUXCamInit");
                    Logger.d(this, TAG, "Test event logged, assuming UXCam initialized");
                    isUXCamInitialized = true;
                } catch (Exception e) {
                    Logger.e(this, TAG, "Failed to log test event, cached key may be invalid: " + e.getMessage());
                    // Ключ, вероятно, неверный, пробуем загрузить новый
                    fetchUXCamKeyFromFirestore(context, attempt + 1);
                }
            } else {
                fetchUXCamKeyFromFirestore(context, attempt);
            }
        } catch (GeneralSecurityException | IOException e) {
            Logger.e(this, TAG, "Error accessing SecurePrefs: " + e.getMessage());
            fetchUXCamKeyFromFirestore(getApplicationContext(), attempt + 1);
        }
    }

    private void fetchUXCamKeyFromFirestore(Context context, int attempt) {

        firestoreHelper.getUixCamKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String uKey) {
                if (uKey == null || uKey.isEmpty()) {
                    Logger.e(context, TAG, "Received invalid UXCam key");
                    fetchUXCamKey(attempt + 1); // Пробуем ещё раз
                    return;
                }

                try {
                    SecurePrefs.saveKey(context, uKey);
                    Logger.d(context, TAG, "UXCam key saved successfully");

                    if (!isUXCamInitialized) {
                        UXConfig config = new UXConfig.Builder(uKey)
                                .enableAutomaticScreenNameTagging(true)
                                .enableImprovedScreenCapture(true)
                                .build();

                        UXCam.startWithConfiguration(config);

                        // Проверяем успешность инициализации
                        try {
                            UXCam.logEvent("TestUXCamInit");
                            Logger.d(context, TAG, "Test event logged, UXCam initialized with new key");
                            isUXCamInitialized = true;
                        } catch (Exception e) {
                            Logger.e(context, TAG, "Failed to log test event with new key: " + e.getMessage());
                            fetchUXCamKey(attempt + 1); // Пробуем ещё раз
                        }
                    }
                } catch (GeneralSecurityException | IOException e) {
                    Logger.e(context, TAG, "Error initializing UXCam or saving key: " + e.getMessage());
                    fetchUXCamKey(attempt + 1);
                } catch (Exception e) {
                    Logger.e(context, TAG, "Unexpected error: " + e.getMessage());
                    fetchUXCamKey(attempt + 1);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Logger.e(context, TAG, "Failed to fetch UXCam key: " + e.getMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
                try {
                    String cachedKey = SecurePrefs.getKey(context);
                    if (cachedKey != null && !isUXCamInitialized) {
                        Logger.d(context, TAG, "Using cached UXCam key after Firestore failure");
                        UXConfig config = new UXConfig.Builder(cachedKey)
                                .enableAutomaticScreenNameTagging(true)
                                .enableImprovedScreenCapture(true)
                                .build();

                        UXCam.startWithConfiguration(config);

                        try {
                            UXCam.logEvent("TestUXCamInit");
                            Logger.d(context, TAG, "Test event logged, UXCam initialized with cached key");
                            isUXCamInitialized = true;
                        } catch (Exception ex) {
                            Logger.e(context, TAG, "Failed to log test event with cached key: " + ex.getMessage());
                            fetchUXCamKey(attempt + 1);
                        }
                    } else {
                        fetchUXCamKey(attempt + 1);
                    }
                } catch (GeneralSecurityException | IOException ex) {
                    Logger.e(context, TAG, "Error using cached key: " + ex.getMessage());
                    fetchUXCamKey(attempt + 1);
                }
            }
        });
    }



    @SuppressLint("SourceLockedOrientationActivity")
    private void setDefaultOrientation() {
        // Установка ориентации экрана в портретный режим
        // Это может не сработать для всех активити
        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    // Для получения текущей активити (необходимый метод, чтобы использовать его в setDefaultOrientation)
    public static Activity getCurrentActivity() {
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
        // Проверяем, было ли обновление приложения
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedVersion = prefs.getString("app_version", "");
        String currentVersion;

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            currentVersion = "";
            Logger.e(getApplicationContext(), TAG, "Failed to get package info: " + e.getMessage());
        }

        // Если версия изменилась, сохраняем новую и откладываем запуск ANRWatchDog
        assert currentVersion != null;
        if (!currentVersion.equals(savedVersion)) {
            prefs.edit().putString("app_version", currentVersion).apply();
            // Откладываем запуск ANRWatchDog на 10 секунд
            new Handler(Looper.getMainLooper()).postDelayed(this::startANRWatchDog, 10_000);
        } else {
            // Если обновления не было, запускаем сразу
            startANRWatchDog();
        }
    }

    private void startANRWatchDog() {
        new ANRWatchDog(4000)
                .setANRListener(error -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplicationContext(), R.string.anr_message, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), AnrActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                    Logger.e(getApplicationContext(), TAG, "ANR occurred: " + error.toString());
                    FirebaseCrashlytics.getInstance().recordException(error);
                })
                .start();
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
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                startMemoryMonitoring();
                if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                    if (MainActivity.currentNavDestination != R.id.nav_restart) {
                        MainActivity.currentNavDestination = R.id.nav_restart; // Устанавливаем текущий экран
                        MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_restart, true)
                                .build());
                    }
                    return;
                }

                isAppInForeground = true;
                if (idleTimeoutManager != null) {
                    idleTimeoutManager.resetTimer();
                }
            }


            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                isAppInForeground = false;
//                backgroundStartTime = System.currentTimeMillis();
                stopMemoryMonitoring(); // Останавливаем мониторинг при паузе
                currentActivity = null;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                firestoreHelper.stopListening();
                if (currentActivity == activity) {
                    currentActivity = null;
                }
            }
        });
    }

    private final Handler memoryCheckHandler = new Handler();
    private final Runnable memoryCheckRunnable = new Runnable() {
        @Override
        public void run() {
            // Выполняем проверку памяти
            if (currentActivity != null) {
                checkMemoryUsage(currentActivity);
            }
            // Повторяем выполнение через 5 секунд (5000 миллисекунд)
            memoryCheckHandler.postDelayed(this, 5000);
        }
    };

    // Запуск мониторинга
    public void startMemoryMonitoring() {
        memoryCheckHandler.post(memoryCheckRunnable);
    }

    // Остановка мониторинга
    public void stopMemoryMonitoring() {
        memoryCheckHandler.removeCallbacks(memoryCheckRunnable);
    }



    private void checkMemoryUsage(Activity activity) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);

        if (memoryInfo.lowMemory) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMemoryWarningTime > 60 * 1000) { // Уведомление раз в 60 секунд
                // Отобразите уведомление пользователю

                String message = getString(R.string.low_memory_0) + memoryInfo.availMem + getString(R.string.low_memory_1) + memoryInfo.lowMemory;
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();

                lastMemoryWarningTime = currentTime;

            }


        }


        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInternetWarningTime > 30 * 1000) { // Уведомление раз в 30 секунд
            NetworkUtils.isInternetStable(new NetworkUtils.ApiCallback() {
                @Override
                public void onSuccess(boolean isStable) {
                    if (isStable) {
                        Logger.d(activity,"NetworkCheck", "Internet is stable.");
                    } else {
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity, R.string.low_connect, Toast.LENGTH_SHORT).show()
                        );
                        Logger.d(activity,"NetworkCheck", "Internet is unstable.");
                    }

                    // Запуск Toast в основном потоке


                    lastInternetWarningTime = currentTime;
                }

                @Override
                public void onFailure(Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                    Logger.e(activity,"NetworkCheck", "Error checking internet stability." + t);
                }
            });

        }


        Logger.d(activity,"MemoryMonitor", "Свободная память: " + memoryInfo.availMem + " байт");
        Logger.d(activity,"MemoryMonitor", "Состояние нехватки памяти: " + memoryInfo.lowMemory);
    }


    private void visicomKeyFromFb()
    {

        firestoreHelper.getVisicomKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String vKey) {
                // Обработка успешного получения ключа
                MainActivity.apiKey = vKey;
                Logger.d(getApplicationContext(),TAG, "Visicom Key: " + vKey);
            }

            @Override
            public void onFailure(Exception e) {
                // Обработка ошибок
                FirebaseCrashlytics.getInstance().recordException(e);
                Logger.e(getApplicationContext(),TAG, "Ошибка: " + e.getMessage());
            }
        });

    }

    private void mapboxKeyFromFb()
    {
        firestoreHelper.getMapboxKey(new FirestoreHelper.OnMapboxKeyFetchedListener() {
            @Override
            public void onSuccess(String mKey) {
                // Обработка успешного получения ключа
                MainActivity.apiKeyMapBox = mKey;
                Logger.d(getApplicationContext(),TAG, "Mapbox Key: " + MainActivity.apiKeyMapBox);
            }

            @Override
            public void onFailure(Exception e) {
                // Обработка ошибок
                FirebaseCrashlytics.getInstance().recordException(e);
                Logger.e(getApplicationContext(),TAG, "Ошибка: " + e.getMessage());
            }
        });

    }
}
