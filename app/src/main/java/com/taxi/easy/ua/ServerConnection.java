package com.taxi.easy.ua;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerConnection {

    public interface ConnectionCallback {
        void onConnectionResult(boolean isConnected);
    }

    public static void checkConnection(String url, ConnectionCallback callback) {
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE); // Установите уровень логирования FINE

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean isConnected = response.isSuccessful();
                callback.onConnectionResult(isConnected);

                response.close(); // Закрыть тело ответа
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e); // Вывести исключение для отладки
                callback.onConnectionResult(false);
            }
        });
    }

}
