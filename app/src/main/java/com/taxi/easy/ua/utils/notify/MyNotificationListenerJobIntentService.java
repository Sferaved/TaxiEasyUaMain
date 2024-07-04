package com.taxi.easy.ua.utils.notify;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import androidx.core.app.JobIntentService;

public class MyNotificationListenerJobIntentService extends JobIntentService {
    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, MyNotificationListenerJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // Ваш код сервиса здесь
        NotificationUtils.disableNotificationChannel(getApplicationContext(), "ForegroundServiceChannel");
    }
}

