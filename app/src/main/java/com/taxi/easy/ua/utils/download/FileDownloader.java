package com.taxi.easy.ua.utils.download;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FileDownloader {
    private static final OkHttpClient client = new OkHttpClient();

    public interface DownloadCallback {
        void onDownloadComplete(String filePath);

        void onDownloadFailed(Exception e);
    }

    public static void downloadFile(String url, String saveFilePath, DownloadCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onDownloadFailed(e);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    try (InputStream inputStream = responseBody.byteStream();
                         FileOutputStream fos = new FileOutputStream(saveFilePath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }

                        // Вызываем колбэк с путем загруженного файла
                        if (callback != null) {
                            callback.onDownloadComplete(saveFilePath);
                        }

                        File file = new File(saveFilePath);

                        if (file.exists() && file.isFile()) {
                            // Файл скачан успешно
                            Log.d("TAG", "File downloaded successfully: " + file.getAbsolutePath());
                            // Здесь вы можете запустить установку файла
                            // installFile(file.getAbsolutePath());
                        } else {
                            // Файл не был успешно скачан
                            Log.d("TAG", "File download failed.");
                        }
                    } catch (IOException e) {
                        if (callback != null) {
                            callback.onDownloadFailed(e);
                        }
                    } finally {
                        responseBody.close();
                    }
                }
            }
        });
    }
}


