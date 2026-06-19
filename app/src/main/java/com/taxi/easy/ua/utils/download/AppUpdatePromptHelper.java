package com.taxi.easy.ua.utils.download;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * Решает, показывать ли пользователю диалог немедленного обновления
 * после ответа Play Core {@link AppUpdateInfo}.
 */
public final class AppUpdatePromptHelper {

    private AppUpdatePromptHelper() {
    }

    public static boolean shouldShowUpdateDialog(AppUpdateInfo appUpdateInfo) {
        if (appUpdateInfo == null) {
            return false;
        }
        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            return false;
        }
        if (!appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            return false;
        }
        int installStatus = appUpdateInfo.installStatus();
        return installStatus == InstallStatus.PENDING
                || installStatus == InstallStatus.UNKNOWN
                || installStatus == InstallStatus.INSTALLED
                || installStatus == InstallStatus.FAILED
                || installStatus == InstallStatus.CANCELED
                || installStatus == InstallStatus.DOWNLOADED;
    }
}
