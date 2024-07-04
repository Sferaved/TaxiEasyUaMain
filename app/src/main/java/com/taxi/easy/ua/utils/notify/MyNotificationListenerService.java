package com.taxi.easy.ua.utils.notify;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyNotificationListenerService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Вызываем метод для отключения уведомлений для канала "ForegroundServiceChannel"
        NotificationUtils.disableNotificationChannel(getApplicationContext(), "ForegroundServiceChannel");

        // Возвращаем флаг, указывающий на то, что сервис не должен быть остановлен, пока он активен
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Метод onBind не используется для стартового сервиса
        return null;
    }
}

