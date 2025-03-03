package com.taxi.easy.ua.utils.download;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

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

    private static void restartApplication(Context context) {
        int sdkVersion = android.os.Build.VERSION.SDK_INT;
        if (sdkVersion >= android.os.Build.VERSION_CODES.TIRAMISU) {
            restartWithAlarmManager(context);
        } else {
            restartActivity(context);
        }
    }

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