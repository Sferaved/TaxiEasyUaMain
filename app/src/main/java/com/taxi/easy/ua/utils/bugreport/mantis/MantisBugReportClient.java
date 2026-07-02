package com.taxi.easy.ua.utils.bugreport.mantis;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.taxi.easy.ua.utils.network.RetryInterceptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MantisBugReportClient {

    private static final String TAG = "MantisBugReportClient";

    public int createIssue(@NonNull MantisConfig config,
                           @NonNull String summary,
                           @NonNull String description,
                           @Nullable File logFile) throws IOException {
        MantisIssueModels.CreateIssueRequest request = new MantisIssueModels.CreateIssueRequest();
        request.summary = summary;
        request.description = description;
        request.project = new MantisIssueModels.IdRef(config.projectId);
        request.category = new MantisIssueModels.IdRef(config.categoryId);

        if (logFile != null && logFile.exists() && logFile.length() > 0) {
            request.files = Collections.singletonList(
                    new MantisIssueModels.FileAttachment(logFile.getName(), encodeFileToBase64(logFile))
            );
        }

        MantisApiService apiService = buildApiService(config);
        Response<MantisIssueModels.CreateIssueResponse> response =
                apiService.createIssue(request).execute();

        if (!response.isSuccessful() || response.body() == null || response.body().issue == null) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
            Log.e(TAG, "Mantis create issue failed: code=" + response.code() + " body=" + errorBody);
            throw new IOException("Mantis HTTP " + response.code());
        }

        return response.body().issue.id;
    }

    @NonNull
    private static MantisApiService buildApiService(@NonNull MantisConfig config) {
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request authenticated = original.newBuilder()
                    .header("Authorization", config.apiToken)
                    .header("Content-Type", "application/json")
                    .build();
            return chain.proceed(authenticated);
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(new RetryInterceptor())
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MantisIssueUrlHelper.buildRetrofitBaseUrl(config.baseUrl))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(MantisApiService.class);
    }

    @NonNull
    private static String encodeFileToBase64(@NonNull File file) throws IOException {
        byte[] fileContent = new byte[(int) file.length()];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int read = inputStream.read(fileContent);
            if (read != fileContent.length) {
                throw new IOException("Failed to read log file completely");
            }
        }
        return Base64.encodeToString(fileContent, Base64.NO_WRAP);
    }
}
