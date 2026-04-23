package com.taxi.easy.ua.utils.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    private static final String CHAT_ID_TAXI = "1379298637"; // Замените на ваш chat ID

    // Логирование на всех этапах работы с сетью
    private static final String TAG = "TelegramUtils";

    // Метод для отправки сообщения об ошибке и лог-файла в Telegram
    public static void sendErrorToTelegram(String errorMessage, String logFilePath) {
        Log.d(TAG, "Started sending error message to Telegram...");

        File logFile = new File(logFilePath);

        if (!logFile.exists() || !logFile.canRead()) {
            Log.e(TAG, "Log file invalid: " + logFilePath);
            return;
        }

        // Читаем файл один раз в память
        byte[] fileContent;
        try (FileInputStream fis = new FileInputStream(logFile)) {
            fileContent = new byte[(int) logFile.length()];
            fis.read(fileContent);
            Log.d(TAG, "File read successfully. Size: " + fileContent.length + " bytes");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read log file: " + e.getMessage(), e);
            return;
        }

        // Создаем клиент и Retrofit
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.telegram.org/bot" + TOKEN + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TelegramApiService apiService = retrofit.create(TelegramApiService.class);

        // Отправляем в оба чата, используя одни и те же данные
        sendDocumentToChat(apiService, CHAT_ID, errorMessage, logFile.getName(), fileContent);
        sendDocumentToChat(apiService, CHAT_ID_TAXI, errorMessage, logFile.getName(), fileContent);
    }

    private static void sendDocumentToChat(TelegramApiService apiService, String chatId,
                                           String errorMessage, String fileName, byte[] fileContent) {
        try {
            // Создаем Body из байтов (каждый запрос получает свежий RequestBody)
            RequestBody fileBody = RequestBody.create(fileContent, MediaType.parse("application/octet-stream"));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("document", fileName, fileBody);

            RequestBody messageBody = RequestBody.create(errorMessage, MediaType.parse("text/plain"));

            Log.d(TAG, "Sending to chat: " + chatId);

            apiService.sendDocument(chatId, messageBody, filePart).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Success to " + chatId + " - Code: " + response.code());
                    } else {
                        Log.e(TAG, "Failed to " + chatId + " - Code: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failure to " + chatId + ": " + t.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error sending to " + chatId + ": " + e.getMessage(), e);
        }
    }

}
