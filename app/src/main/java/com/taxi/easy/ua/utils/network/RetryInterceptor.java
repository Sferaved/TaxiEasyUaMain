package com.taxi.easy.ua.utils.network;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {
    private static final String TAG = "RetryInterceptor";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException exception = null;

        int tryCount = 0;
        while (tryCount < MAX_RETRIES) {
            try {
                Logger.d(MyApplication.getContext(), TAG, "Попытка #" + (tryCount + 1) + " → " + request.url());
                response = chain.proceed(request);

                int code = response.code();
                if (response.isSuccessful()) {
                    return response;
                } else {
                    if (code >= 500) {
                        Logger.w(MyApplication.getContext(), TAG, "Ошибка сервера (5xx): " + code);
                    } else if (code >= 400) {
                        Logger.w(MyApplication.getContext(), TAG, "Ошибка клиента (4xx): " + code);
                    } else {
                        Logger.w(MyApplication.getContext(), TAG, "Прочий код ответа: " + code);
                    }
                    response.close();
                }
            } catch (IOException e) {
                exception = e;
                Logger.e(MyApplication.getContext(), TAG, "Сетевая ошибка попытки #" + (tryCount + 1) + ": "
                        + e.getClass().getSimpleName() + " → " + e.getMessage());
            }

            tryCount++;
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.e(MyApplication.getContext(), TAG, "Повтор прерван: " + ie);
                throw new IOException("Повтор прерван", ie);
            }
        }

        if (response != null) {
            Logger.e(MyApplication.getContext(), TAG, "Неудачный ответ после " + MAX_RETRIES + " попыток. Код: " + response.code());
            return response;
        } else {
            Logger.e(MyApplication.getContext(), TAG, "Запрос не выполнен после " + MAX_RETRIES + " попыток. Последняя ошибка: "
                    + exception.getClass().getSimpleName() + " → " + exception.getMessage());
            throw exception;
        }
    }
}
