package com.taxi.easy.ua.utils.test.kafka;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TestKafkaRequest {

    private static final String TAG = "TestKafkaRequest";
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Отправка тестового GET запроса в Laravel
     *
     * @param orderId - ID заказа
     * @param status  - статус заказа
     */
    public void sendTestMessage(String orderId, String status) {
        // Формируем URL
        String url = "https://t.easy-order-taxi.site/kafka/test-kafka/" + orderId + "/" + status;

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



    // Пример использования в Android приложении
    public static void testKafkaMessage() {
        TestKafkaRequest test = new TestKafkaRequest();
        test.sendTestMessage("123456", "new");
    }
}
