package com.taxi.easy.ua.utils.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TelegramUtils {

    private static final String BASE_URL = "https://api.telegram.org/bot";
    private static final String TOKEN = "7012302264:AAG-uGMIt4xBQLGznvXXR0VkqtNsXw462gg"; // Замените на ваш токен
    private static final String CHAT_ID = "120352595"; // Замените на ваш chat ID

    // Логирование на всех этапах работы с сетью
    private static final String TAG = "TelegramUtils";

    // Метод для отправки сообщения об ошибке и лог-файла в Telegram
    public static void sendErrorToTelegram(String errorMessage, String logFilePath) {
        // Логирование начала работы метода
        Log.d(TAG, "Started sending error message to Telegram...");

        // Чтение содержимого лог-файла
        File logFile = new File(logFilePath);

        if (!logFile.exists()) {
            Log.e(TAG, "Log file does not exist: " + logFilePath);
            return;
        }

        if (!logFile.canRead()) {
            Log.e(TAG, "Log file cannot be read: " + logFilePath);
            return;
        }

        Log.d(TAG, "Preparing to send log file: " + logFile.getName());

        // Создаем клиент с тайм-аутами
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Log.d(TAG, "OkHttpClient created with timeouts: 30 seconds for connect, write, and read.");

        // Создаем Retrofit с кастомным клиентом
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.telegram.org/bot" + TOKEN + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Log.d(TAG, "Retrofit instance created.");

        TelegramApiService apiService = retrofit.create(TelegramApiService.class);
        Log.d(TAG, "TelegramApiService created.");

        // Создаем запрос для отправки сообщения с прикрепленным файлом
        RequestBody fileBody = RequestBody.create(logFile, MediaType.parse("application/octet-stream"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("document", logFile.getName(), fileBody);


        Log.d(TAG, "MultipartBody.Part created for the log file.");

        // Создаем описание для сообщения
        RequestBody messageBody = RequestBody.create(errorMessage, MediaType.parse("text/plain"));


        Log.d(TAG, "Message body created with error details.");

        // Отправка сообщения через Telegram API
        Call<Void> call = apiService.sendDocument(CHAT_ID, messageBody, filePart);

        Log.d(TAG, "Sending request to Telegram API...");

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "Response received. Code: " + response.code());

            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to send error message: " + t.getMessage());

            }
        });
    }
}
