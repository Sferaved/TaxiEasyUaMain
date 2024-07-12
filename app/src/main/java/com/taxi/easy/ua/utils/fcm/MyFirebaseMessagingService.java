package com.taxi.easy.ua.utils.fcm;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.fcm.token_send.ApiServiceToken;
import com.taxi.easy.ua.utils.fcm.token_send.RetrofitClientToken;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.notify.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessagingService";
    private static final String PREFS_NAME = "UserTokenPrefs";
    private static final String TOKEN_KEY = "token";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Logger.d(this, TAG,  "New token: " + token);
        updateUserTokenPrefs(token);
    }
    private void updateUserTokenPrefs(String token) {


        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Logger.d(this, TAG, "token" + token);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(TOKEN_KEY, token);
        editor.apply();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Обработка входящего сообщения
        if (remoteMessage.getNotification() != null) {
            // Получаем текст сообщения
            String message = remoteMessage.getNotification().getBody();
            Logger.d(getApplicationContext(), TAG, message);
            // Уведомляем пользователя
            notifyUser(message);
        }
//        else {
//            // Обработка данных из сообщения
//            Map<String, String> data = remoteMessage.getData();
//            // Пример обработки данных
//            String customData = data.get("customDataKey");
//            notifyUser("Received custom data: " + customData);
//        }
    }



    private void notifyUser (String message) {

        String title = getApplicationContext().getString(R.string.new_message)
                + " " + getApplicationContext().getString(R.string.app_name) ;

        NotificationHelper.showNotificationMessage(getApplicationContext(), title, message);

    }
}
