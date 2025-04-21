package com.taxi.easy.ua.utils.download;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.log.Logger;

public class AppUpdater {

    private static final String TAG = "AppUpdater";
    private AppUpdateManager appUpdateManager;
    private final Activity activity;
    private static final int MY_REQUEST_CODE = 100;
    private OnUpdateListener onUpdateListener;
    private static final int REQUEST_CODE_EXACT_ALARM = 1001;
    private static final int REQUEST_CODE_BATTERY_OPTIMIZATION = 1002;
    public AppUpdater(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity must not be null");
        }
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        this.onUpdateListener = onUpdateListener;
    }

    public void checkForUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            Logger.d(activity, TAG, "Update availability: " + appUpdateInfo.updateAvailability());
            Logger.d(activity, TAG, "Update priority: " + appUpdateInfo.updatePriority());
            Logger.d(activity, TAG, "Client version staleness days: " + appUpdateInfo.clientVersionStalenessDays());

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Logger.d(activity, TAG, "Available updates found");
                try {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo, AppUpdateType.IMMEDIATE, activity, MY_REQUEST_CODE);
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo, AppUpdateType.FLEXIBLE, activity, MY_REQUEST_CODE);
                    }
                } catch (Exception e) {
                    Logger.e(activity, TAG, "Failed to start update flow: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);
                    Toast.makeText(activity, R.string.update_error, Toast.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(e -> {
            Logger.e(activity, TAG, "Failed to check for updates: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        });
    }

    public void startUpdate() {
        Logger.d(activity, TAG, "Starting app update process");
        requestDisableBatteryOptimization(activity);

        setOnUpdateListener(() -> {
            Toast.makeText(activity, R.string.update_finish_mes, Toast.LENGTH_SHORT).show();
            restartApplication(activity);
        });
        registerListener();
        checkForUpdate();
    }

    private final InstallStateUpdatedListener installStateUpdatedListener = state -> {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {

            appUpdateManager.completeUpdate()
                    .addOnSuccessListener(aVoid -> {
                        if (onUpdateListener != null) {
                            onUpdateListener.onUpdateCompleted();
                        }
                    })
                    .addOnFailureListener(e -> {
                        FirebaseCrashlytics.getInstance().recordException(e);
                    });
        }
    };

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private static void restartApplication(Context context) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
                    } else {
                        requestExactAlarmPermission(context);
                        return;
                    }
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
                }
            } else {
                context.startActivity(intent);
            }

            if (context instanceof Activity) {
                ((Activity) context).finish();
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                System.exit(0);
            }, 600);
        } catch (Exception e) {
            Logger.e(context, TAG, "Error restarting application: " + e.getMessage());
            try {
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                Logger.e(context, TAG, "Fallback restart failed: " + ex.getMessage());
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static void requestExactAlarmPermission(Context context) {
        if (context instanceof Activity) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            ((Activity) context).startActivityForResult(intent, REQUEST_CODE_EXACT_ALARM);
        }
    }

    private static void requestDisableBatteryOptimization(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
            @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION);
            }
        }
    }



//    private static void restartApplication(Context context) {
//        int sdkVersion = android.os.Build.VERSION.SDK_INT;
//        if (sdkVersion >= android.os.Build.VERSION_CODES.TIRAMISU) {
//            restartWithAlarmManager(context);
//        } else {
//            restartActivity(context);
//        }
//    }

    private static void restartWithAlarmManager(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);
        }

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        }, 500);
    }

    private static void restartActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        }, 500);
    }

    public void registerListener() {
        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    public void unregisterListener() {
        if (appUpdateManager != null) {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }

    public interface OnUpdateListener {
        void onUpdateCompleted();
    }
}