package com.taxi.easy.ua.utils.helpers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private static final String TOKEN = "7012302264:AAG-uGMIt4xBQLGznvXXR0VkqtNsXw462gg";
    private static final String CHAT_ID = "120352595";
    private static final String CHAT_ID_TAXI = "1379298637";

    private static final String TAG = "TelegramUtils";
    private static final int MAX_CAPTION_LENGTH = 1000;

    public static void sendErrorToTelegram(String errorMessage, @Nullable String logFilePath) {
        Log.d(TAG, "Started sending error message to Telegram...");

        // Обрезаем сообщение для caption
        String caption = errorMessage;

        if (caption.length() > MAX_CAPTION_LENGTH) {
            caption = caption.substring(0, MAX_CAPTION_LENGTH - 50) + "\n\n... [сообщение обрезано из-за лимита Telegram]";
            Log.w(TAG, "Caption truncated from " + errorMessage.length() + " to " + caption.length() + " chars");
        }

        Log.d(TAG, "Caption length: " + caption.length() + " chars");

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.telegram.org/bot" + TOKEN + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TelegramApiService apiService = retrofit.create(TelegramApiService.class);

        // Отправляем файл если есть
        if (logFilePath != null) {
            File logFile = new File(logFilePath);
            if (logFile.exists() && logFile.canRead() && logFile.length() > 0) {
                Log.d(TAG, "Sending log file: " + logFilePath + ", size: " + logFile.length() + " bytes");
                sendDocumentToChat(apiService, CHAT_ID, caption, logFile);
                sendDocumentToChat(apiService, CHAT_ID_TAXI, caption, logFile);
                return;
            } else {
                Log.w(TAG, "Log file invalid, sending message only");
            }
        }

        // Отправляем только сообщение
        sendMessageOnly(apiService, caption);
    }

    private static void sendDocumentToChat(TelegramApiService apiService, String chatId,
                                           String caption, File logFile) {
        try {
            // Читаем файл
            byte[] fileContent;
            try (FileInputStream fis = new FileInputStream(logFile)) {
                fileContent = new byte[(int) logFile.length()];
                fis.read(fileContent);
            }

            // Создаем RequestBody для файла
            RequestBody fileBody = RequestBody.create(fileContent, MediaType.parse("application/octet-stream"));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("document", logFile.getName(), fileBody);

            // Для caption: если слишком длинный, отправляем короткий
            String finalCaption = caption;
            if (finalCaption.length() > MAX_CAPTION_LENGTH) {
                finalCaption = "📋 Баг-репорт (файл с логами)\n\nФайл с логами прикреплен ниже.";
                Log.d(TAG, "Using short caption for file");
            }

            RequestBody captionBody = RequestBody.create(finalCaption, MediaType.parse("text/plain"));
            RequestBody chatIdBody = RequestBody.create(chatId, MediaType.parse("text/plain"));

            Log.d(TAG, "Sending document to chat: " + chatId + ", caption length: " + finalCaption.length());

            // Пробуем отправить документ
            Call<Void> call = apiService.sendDocument(chatIdBody, captionBody, filePart);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Document sent successfully to " + chatId);
                    } else {
                        Log.e(TAG, "Failed to send document to " + chatId + " - Code: " + response.code());
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "Error body: " + errorBody);

                                // Если ошибка с caption, пробуем отправить файл без caption
                                if (errorBody.contains("caption is too long")) {
                                    Log.w(TAG, "Caption too long, retrying without caption");
                                    sendDocumentWithoutCaption(apiService, chatId, logFile);
                                } else {
                                    // Отправляем только сообщение
                                    sendMessageOnly(apiService, caption);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error: " + e.getMessage());
                            sendMessageOnly(apiService, caption);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failure sending document to " + chatId + ": " + t.getMessage());
                    sendMessageOnly(apiService, caption);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error sending document: " + e.getMessage(), e);
            sendMessageOnly(apiService, caption);
        }
    }

    /**
     * Отправка файла без caption
     */
    private static void sendDocumentWithoutCaption(TelegramApiService apiService, String chatId, File logFile) {
        try {
            byte[] fileContent;
            try (FileInputStream fis = new FileInputStream(logFile)) {
                fileContent = new byte[(int) logFile.length()];
                fis.read(fileContent);
            }

            RequestBody fileBody = RequestBody.create(fileContent, MediaType.parse("application/octet-stream"));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("document", logFile.getName(), fileBody);
            RequestBody chatIdBody = RequestBody.create(chatId, MediaType.parse("text/plain"));

            Log.d(TAG, "Sending document without caption to " + chatId);

            Call<Void> call = apiService.sendDocumentWithoutCaption(chatIdBody, filePart);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Document (no caption) sent successfully to " + chatId);
                        sendMessageOnly(apiService, "📋 Баг-репорт\n\nФайл с логами прикреплен выше.");
                    } else {
                        Log.e(TAG, "Failed to send document (no caption) to " + chatId);
                        sendMessageOnly(apiService, "📋 Баг-репорт\n\n✉️ Сообщение отправлено, но файл не загрузился.");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failure sending document (no caption): " + t.getMessage());
                    sendMessageOnly(apiService, "📋 Баг-репорт");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error sending document without caption: " + e.getMessage());
            // Отправляем сообщение без файла
            sendMessageOnly(apiService, "📋 Баг-репорт\n\n⚠️ Не удалось отправить файл с логами.");
        }
    }

    private static void sendMessageOnly(TelegramApiService apiService, String message) {
        String finalMessage = message;
        if (finalMessage.length() > 4000) {
            finalMessage = finalMessage.substring(0, 3997) + "\n\n... [сообщение обрезано]";
            Log.w(TAG, "Message truncated from " + message.length() + " to 4000 chars");
        }

        sendMessageToChat(apiService, CHAT_ID, finalMessage);
        sendMessageToChat(apiService, CHAT_ID_TAXI, finalMessage);
    }

    private static void sendMessageToChat(TelegramApiService apiService, String chatId, String message) {
        try {
            Log.d(TAG, "Sending message to chat: " + chatId + ", length: " + message.length());

            apiService.sendMessage(chatId, message).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Message sent successfully to " + chatId);
                    } else {
                        Log.e(TAG, "Failed to send message to " + chatId + " - Code: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failure sending message to " + chatId + ": " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending message to " + chatId + ": " + e.getMessage());
        }
    }
}