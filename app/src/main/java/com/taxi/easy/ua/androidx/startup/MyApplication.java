package com.taxi.easy.ua.androidx.startup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.github.anrwatchdog.ANRWatchDog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.exit.AnrActivity;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.keys.SecurePrefs;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;
import com.taxi.easy.ua.utils.time_ut.IdleTimeoutManager;
import com.taxi.easy.ua.utils.worker.OrderStatusWorker;
import com.uxcam.UXCam;
import com.uxcam.datamodel.UXConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;

public class MyApplication extends Application {

    private final String TAG = "MyApplication";
    @SuppressLint("StaticFieldLeak")
    private static MyApplication instance;
    @SuppressLint("StaticFieldLeak")
    private static Activity currentActivity = null;

    public static SharedPreferencesHelper sharedPreferencesHelperMain;
    private IdleTimeoutManager idleTimeoutManager;

    private boolean isUXCamInitialized = false;
    private static final int MAX_RETRY_ATTEMPTS = 2;

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    FirestoreHelper firestoreHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
        Log.d("LocaleDebug", "Current locale: " + currentLocale);

        try {
            initializeFirebaseAndCrashlytics();
            setDefaultOrientation();
            sharedPreferencesHelperMain = new SharedPreferencesHelper(this);

            firestoreHelper = new FirestoreHelper(this);
            firestoreHelper.listenForResponseChanges();

            registerActivityLifecycleCallbacks(); // теперь запускаем/останавливаем WorkManager
            setupCrashHandler();
            setupANRWatchDog();
            fetchUXCamKey(1);

            visicomKeyFromFb();
            mapboxKeyFromFb();
            supportEmailFromFb();

        } catch (Exception e) {
            Logger.e(this, TAG, "Initialization failed: " + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    // ---------- ЗАПУСК И ОСТАНОВКА WORKER ----------

    private void startOrderStatusWorker() {
        Logger.d(getApplicationContext(), "MyApplication", "Запускаем OrderStatusWorker");
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(OrderStatusWorker.class).build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "OrderStatusWorker",
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }


    private void stopOrderStatusWorker() {
        Logger.d(getApplicationContext(), "MyApplication", "Останавливаем OrderStatusWorker");
        WorkManager.getInstance(getApplicationContext()).cancelUniqueWork("OrderStatusWorker");
    }


    // ---------- LIFE CYCLE CALLBACKS ----------

    private void registerActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                currentActivity = activity;
                idleTimeoutManager = new IdleTimeoutManager(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    Logger.d(getApplicationContext(), TAG, "Приложение на переднем плане");
                    stopOrderStatusWorker(); // останавливаем фоновые опросы
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (idleTimeoutManager != null) {
                    idleTimeoutManager.resetTimer();
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) { }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    Logger.d(getApplicationContext(), TAG, "Приложение в фоне");
                    startOrderStatusWorker(); // запускаем фоновый опрос
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                firestoreHelper.stopListening();
                if (currentActivity == activity) {
                    currentActivity = null;
                }
                // Проверяем, что все активности уничтожены и приложение закрыто
                if (activityReferences == 0 && !isActivityChangingConfigurations) {
                    Logger.d(getApplicationContext(), TAG, "Приложение полностью закрыто");
                    stopOrderStatusWorker(); // останавливаем воркер
                }
            }
        });
    }

    // ---------- ПОЛЕЗНЫЕ МЕТОДЫ ----------
    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    private void setDefaultOrientation() {
        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void initializeFirebaseAndCrashlytics() {
        FirebaseApp.initializeApp(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    }

    private void setupCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Логируем крэш
            FirebaseCrashlytics.getInstance().recordException(throwable);
            Logger.e(getApplicationContext(), "CrashHandler", "Crash: " + throwable.getMessage());
            Logger.e(getApplicationContext(), "CrashHandler", Log.getStackTraceString(throwable));

            // Сохраняем стек крэша
            sharedPreferencesHelperMain.saveValue("last_crash", Log.getStackTraceString(throwable));

            // Пытаемся запустить AnrActivity
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Intent intent = new Intent(getApplicationContext(), AnrActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    getApplicationContext().startActivity(intent);
                } catch (Exception e) {
                    Logger.e(getApplicationContext(),"CrashHandler", "Cannot start AnrActivity: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);

                    // Если не удалось — показать уведомление или другой fallback
                    showNotification(getApplicationContext());
                }
            });

            // Через секунду убиваем процесс
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }, 1000);
        });
    }



    private void setupANRWatchDog() {
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
        assert currentVersion != null;
        if (!currentVersion.equals(savedVersion)) {
            prefs.edit().putString("app_version", currentVersion).apply();
        } else {
            startANRWatchDog(this);
        }
    }

    private void startANRWatchDog(Context context) {
        new ANRWatchDog(4000)
                .setANRListener(error -> {
                    Logger.e(context, TAG, "ANR occurred: " + error);
                    FirebaseCrashlytics.getInstance().recordException(error);

                    try {
                        // Запуск AnrActivity напрямую
                        Intent intent = new Intent(context.getApplicationContext(), AnrActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.getApplicationContext().startActivity(intent);
                    } catch (Exception e) {
                        Logger.e(context, TAG, "Failed to start AnrActivity: " + e.getMessage());
                        FirebaseCrashlytics.getInstance().recordException(e);

                        // Если не удалось запустить активность, показываем уведомление
                        showNotification(context);
                    }
                })
                .start();
    }


    private void showNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                "anr_channel", "ANR Notifications", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "anr_channel")
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle(context.getString(R.string.anr_detected))
                .setContentText(context.getString(R.string.application_is_not_responding))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // ---------- FIRESTORE ЗАПРОСЫ ----------
    private void visicomKeyFromFb() {
        firestoreHelper.getVisicomKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String vKey) {
                MainActivity.apiKey = vKey;
                Logger.d(getApplicationContext(), TAG, "Visicom Key: " + vKey);
            }

            @Override
            public void onFailure(Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                Logger.e(getApplicationContext(), TAG, "Ошибка: " + e.getMessage());
            }
        });
    }

    private void supportEmailFromFb() {
        firestoreHelper.getSupportEmail(new FirestoreHelper.OnSupportEmailFetchedListener() {
            @Override
            public void onSuccess(String supportEmail) {
                MainActivity.supportEmail = supportEmail;
                Logger.d(getApplicationContext(), TAG, "supportEmail: " + supportEmail);
            }

            @Override
            public void onFailure(Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                Logger.e(getApplicationContext(), TAG, "Ошибка: " + e.getMessage());
            }
        });
    }

    private void mapboxKeyFromFb() {
        firestoreHelper.getMapboxKey(new FirestoreHelper.OnMapboxKeyFetchedListener() {
            @Override
            public void onSuccess(String mKey) {
                MainActivity.apiKeyMapBox = mKey;
                Logger.d(getApplicationContext(), TAG, "Mapbox Key: " + MainActivity.apiKeyMapBox);
            }

            @Override
            public void onFailure(Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                Logger.e(getApplicationContext(), TAG, "Ошибка: " + e.getMessage());
            }
        });
    }

    private void fetchUXCamKey(int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            Logger.e(this, TAG, "Max retry attempts reached for UXCam key");
            return;
        }
        try {
            String apiKey = SecurePrefs.getKey(this);
            Context context = getApplicationContext();
            if (apiKey != null && !isUXCamInitialized) {
                Logger.d(this, TAG, "Using cached UXCam key");
                UXConfig config = new UXConfig.Builder(apiKey)
                        .enableAutomaticScreenNameTagging(true)
                        .build();

                UXCam.startWithConfiguration(config);

                try {
                    UXCam.logEvent("TestUXCamInit");
                    isUXCamInitialized = true;
                } catch (Exception e) {
                    Logger.e(this, TAG, "Invalid UXCam key: " + e.getMessage());
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
                    fetchUXCamKey(attempt + 1);
                    return;
                }
                try {
                    SecurePrefs.saveKey(context, uKey);
                    if (!isUXCamInitialized) {
                        UXConfig config = new UXConfig.Builder(uKey)
                                .enableAutomaticScreenNameTagging(true)
                                .build();

                        UXCam.startWithConfiguration(config);
                        UXCam.logEvent("TestUXCamInit");
                        isUXCamInitialized = true;
                    }
                } catch (Exception e) {
                    fetchUXCamKey(attempt + 1);
                }
            }

            @Override
            public void onFailure(Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                fetchUXCamKey(attempt + 1);
            }
        });
    }
}
