package com.taxi.easy.ua.utils.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

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



    // Метод для отправки сообщения об ошибке в Telegram
//    public static void sendErrorToTelegram(String errorMessage, String logFilePath) {
//        // Чтение содержимого лог-файла
//        StringBuilder logContent = new StringBuilder();
//        File logFile = new File(logFilePath);
//
//        if (!logFile.exists()) {
//            Log.e(TAG, "Log file does not exist: " + logFilePath);
//            return;
//        }
//
//        if (!logFile.canRead()) {
//            Log.e(TAG, "Log file cannot be read: " + logFilePath);
//            return;
//        }
//
//        Log.d(TAG, "Preparing to send log file: " + logFile.getName());
//
//        // Чтение содержимого файла
//        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                logContent.append(line).append("\n");
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Error reading log file: " + e.getMessage());
//            return;
//        }
//
//        // Объединяем текст ошибки с содержимым лога
//        String message = errorMessage + "\n\nLog File Content:\n" + logContent.toString();
//
//        Log.d(TAG, "Preparing to send error message with log file content");
//
//        // Создаем клиент с тайм-аутами
//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .writeTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
//                .build();
//
//        // Создаем Retrofit с кастомным клиентом
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(BASE_URL + TOKEN + "/")
//                .client(client)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        TelegramApiService apiService = retrofit.create(TelegramApiService.class);
//
//        // Отправка сообщения через Telegram API
//        Call<Void> call = apiService.sendMessage(CHAT_ID, message);
//
//        call.enqueue(new retrofit2.Callback<Void>() {
//            @Override
//            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
//                Log.d(TAG, "Response received. Code: " + response.code());
//
//                if (response.isSuccessful()) {
//                    Log.d(TAG, "Error message sent successfully!");
//                } else {
//                    Log.e(TAG, "Failed to send error message. Response Code: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
//                Log.e(TAG, "Failed to send error message: " + t.getMessage());
//                t.printStackTrace();
//            }
//        });
//    }


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
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("document", logFile.getName(),
                RequestBody.create(MediaType.parse("application/octet-stream"), logFile));

        Log.d(TAG, "MultipartBody.Part created for the log file.");

        // Создаем описание для сообщения
        RequestBody messageBody = RequestBody.create(MediaType.parse("text/plain"), errorMessage);

        Log.d(TAG, "Message body created with error details.");

        // Отправка сообщения через Telegram API
        Call<Void> call = apiService.sendDocument(CHAT_ID, messageBody, filePart);

        Log.d(TAG, "Sending request to Telegram API...");

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "Response received. Code: " + response.code());

                if (response.isSuccessful()) {
                    Log.d(TAG, "Error message and log file sent successfully!");
                } else {
                    Log.e(TAG, "Failed to send error message. Response Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to send error message: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
}
