package com.taxi.easy.ua.utils.kafka;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class KafkaRequest {

    private static final String TAG = "KafkaRequest";
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Отправка тестового GET запроса в Laravel
     *
     * @param urlKafka - urlInp заказа
     */
    public void sendCostMessage(String urlKafka) {
        OkHttpClient client = new OkHttpClient();

        if (urlKafka.startsWith("/")) {
            urlKafka = urlKafka.substring(1);
        }

        String[] parts = urlKafka.split("/");

        // Проверка на длину массива
        if (parts.length < 12) {
            Log.e("KafkaRequest", "Недостаточно параметров в urlKafka: " + urlKafka);
            return;
        }

        String originLatitude  = parts[0];
        String originLongitude = parts[1];
        String toLatitude      = parts[2];
        String toLongitude     = parts[3];
        String tarif           = parts[4];   // сейчас у тебя тут " " (пробел!)
        String phone           = parts[5];
        String user            = parts[6];
        String time            = parts[7];
        String date            = parts[8];
        String services        = parts[9];
        String city            = parts[10];
        String application     = parts[11];



        RequestBody body = new FormBody.Builder()
                .add("originLatitude", originLatitude)
                .add("originLongitude", originLongitude)
                .add("toLatitude", toLatitude)
                .add("toLongitude", toLongitude)
                .add("tarif", tarif.trim()) // убираем пробел
                .add("phone", phone)
                .add("user", user)
                .add("time", time)
                .add("date", date)
                .add("services", services)
                .add("city", city)
                .add("application", application)
                .build();

        Request request = new Request.Builder()
                .url("https://t.easy-order-taxi.site/kafka/sendCostMessageMyApi")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("KafkaRequest", "Ошибка: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("KafkaRequest", "Ответ: " + response.body().string());
            }
        });
    }



    public void sendTestMessage(String orderId, String status) {
        // Формируем URL
        String url = "https://t.easy-order-taxi.site/kafka/test-kafka/" + orderId + "/" + status;
        Log.d(TAG, "url: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Ошибка запроса
                Log.e(TAG, "Ошибка запроса: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Получаем тело ответа
                    String responseBody = response.body().string();
                    Log.d(TAG, "Ответ Laravel: " + responseBody);
                    consumeMessages();
                } else {
                    Log.e(TAG, "Ошибка сервера: " + response.code() + " " + response.message());
                }
            }
        });
    }

    public void consumeMessages() {
        String url = "https://t.easy-order-taxi.site/kafka/consume-kafka";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Ошибка получения сообщений: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    Log.d(TAG, "Сообщения Kafka: " + body);
                } else {
                    Log.e(TAG, "Ошибка сервера при получении сообщений: " + response.code());
                }
            }
        });
    }

}
