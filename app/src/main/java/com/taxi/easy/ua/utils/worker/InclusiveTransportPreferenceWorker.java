package com.taxi.easy.ua.utils.worker;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;

public class InclusiveTransportPreferenceWorker extends Worker {
    private static final String TAG = "InclusiveTransportWorker";
    private static final String PREF_NAME = "user_preferences";
    private static final String KEY_INCLUSIVE_TRANSPORT_ASKED = "inclusive_transport_asked";
    private static final String KEY_INCLUSIVE_TRANSPORT_ENABLED = "inclusive_transport_enabled";

    public InclusiveTransportPreferenceWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Logger.d(getApplicationContext(), TAG, "Worker started");

            SharedPreferences prefs = getApplicationContext()
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

            // Проверяем, спрашивали ли уже пользователя
            boolean alreadyAsked = prefs.getBoolean(KEY_INCLUSIVE_TRANSPORT_ASKED, false);
            Logger.d(getApplicationContext(), TAG, "alreadyAsked: " + alreadyAsked);

            if (!alreadyAsked) {
                Logger.d(getApplicationContext(), TAG, "Отправляем запрос на показ диалога");

                // Сохраняем, что вопрос уже был задан (чтобы не спамить)
                prefs.edit().putBoolean(KEY_INCLUSIVE_TRANSPORT_ASKED, true).apply();

                // Отправляем broadcast через LocalBroadcastManager
                Intent intent = new Intent("ACTION_REQUEST_INCLUSIVE_TRANSPORT");
                intent.putExtra("request_type", "inclusive_transport_preference");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                Logger.d(getApplicationContext(), TAG, "Broadcast отправлен");
            } else {
                Logger.d(getApplicationContext(), TAG, "Вопрос уже задавался ранее");
            }

            return Result.success();
        } catch (Exception e) {
            Logger.e(MyApplication.getContext(), TAG,
                    "Ошибка в InclusiveTransportPreferenceWorker: " + e.getMessage());
            e.printStackTrace();
            return Result.failure();
        }
    }

    // Статический метод для сохранения ответа пользователя
    public static void saveUserPreference(boolean needsInclusiveTransport) {
        sharedPreferencesHelperMain.saveValue(KEY_INCLUSIVE_TRANSPORT_ENABLED, needsInclusiveTransport);
        sharedPreferencesHelperMain.saveValue(KEY_INCLUSIVE_TRANSPORT_ASKED, true);
    }

    // Метод для проверки, нужен ли пользователю инклюзивный транспорт
    public static boolean needsInclusiveTransport() {
        return (boolean) sharedPreferencesHelperMain.getValue(KEY_INCLUSIVE_TRANSPORT_ENABLED, false);
    }

    // Метод для проверки, задавался ли уже вопрос
    public static boolean hasBeenAsked() {
        return (boolean) sharedPreferencesHelperMain.getValue(KEY_INCLUSIVE_TRANSPORT_ASKED, false);
    }
}