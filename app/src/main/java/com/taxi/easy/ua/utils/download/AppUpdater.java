package com.taxi.easy.ua.utils.download;

import android.app.Activity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class AppUpdater {

    private final AppUpdateManager appUpdateManager;
    private final Activity activity;
    private static final int MY_REQUEST_CODE = 100;
    private OnUpdateListener onUpdateListener;

    public AppUpdater(Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        this.onUpdateListener = onUpdateListener;
    }

    public void checkForUpdate() {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        // Гибкое обновление
                        startAppUpdate(appUpdateInfo, AppUpdateType.FLEXIBLE);
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        // Немедленное обновление
                        startAppUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE);
                    }
                }
            }
        });
    }

    private void startAppUpdate(AppUpdateInfo appUpdateInfo, int updateType) {
        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateType, activity, MY_REQUEST_CODE);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    // Слушатель для обновления состояния установки
    private final InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(InstallState state) {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // Обновление загружено
                // Вы можете предложить пользователю перезапустить приложение
                appUpdateManager.completeUpdate();
                if (onUpdateListener != null) {
                    onUpdateListener.onUpdateCompleted();
                }
            }
        }
    };

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
