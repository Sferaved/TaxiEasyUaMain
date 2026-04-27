package com.taxi.easy.ua.utils.network;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {
    private static final String TAG = "RetryInterceptor";
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000; // 1 секунда

    // Ошибки, которые имеют смысл повторять
    private static final Class<?>[] RETRYABLE_EXCEPTIONS = {
            UnknownHostException.class,
            SocketTimeoutException.class,
            java.net.ConnectException.class,
            java.net.NoRouteToHostException.class,
            java.net.PortUnreachableException.class
    };

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        int tryCount = 0;
        IOException lastException = null;

        while (tryCount < MAX_RETRIES) {
            try {
                if (tryCount > 0) {
                    Logger.d(MyApplication.getContext(), TAG,
                            String.format("Повторная попытка #%d/%d для %s", tryCount + 1, MAX_RETRIES, request.url()));
                }

                Response response = chain.proceed(request);

                // Успешный ответ
                if (response.isSuccessful()) {
                    if (tryCount > 0) {
                        Logger.d(MyApplication.getContext(), TAG, "Запрос успешен после " + (tryCount + 1) + " попыток");
                    }
                    return response;
                }

                // Обработка HTTP ошибок
                int code = response.code();
                if (code >= 500 && code < 600 && tryCount < MAX_RETRIES - 1) {
                    // Ошибки сервера - можно повторить
                    Logger.w(MyApplication.getContext(), TAG,
                            String.format("Ошибка сервера %d, повтор через %d мс", code, getDelayMs(tryCount)));
                    response.close();
                    waitBeforeRetry(tryCount);
                    tryCount++;
                    continue;
                } else if (code >= 400 && code < 500) {
                    // Ошибки клиента - не повторяем
                    Logger.w(MyApplication.getContext(), TAG,
                            String.format("Ошибка клиента %d, повтор невозможен", code));
                    return response;
                }

                response.close();

            } catch (UnknownHostException e) {
                // Специальная обработка DNS ошибки
                lastException = e;

                // Проверяем интернет
                if (!isNetworkConnected()) {
                    Logger.e(MyApplication.getContext(), TAG,
                            "Нет интернет соединения, повтор невозможен");
                    throw e;
                }

                // Последняя попытка - не повторяем
                if (tryCount == MAX_RETRIES - 1) {
                    Logger.e(MyApplication.getContext(), TAG,
                            "DNS ошибка после " + MAX_RETRIES + " попыток: " + e.getMessage());
                    throw e;
                }

                Logger.w(MyApplication.getContext(), TAG,
                        String.format("DNS ошибка (попытка %d/%d): %s, повтор через %d мс",
                                tryCount + 1, MAX_RETRIES, e.getMessage(), getDelayMs(tryCount)));

                waitBeforeRetry(tryCount);
                tryCount++;
                continue;

            } catch (SocketTimeoutException e) {
                // Таймаут - повторяем
                lastException = e;

                if (tryCount == MAX_RETRIES - 1) {
                    Logger.e(MyApplication.getContext(), TAG,
                            "Таймаут после " + MAX_RETRIES + " попыток: " + e.getMessage());
                    throw e;
                }

                Logger.w(MyApplication.getContext(), TAG,
                        String.format("Таймаут (попытка %d/%d), повтор через %d мс",
                                tryCount + 1, MAX_RETRIES, getDelayMs(tryCount)));

                waitBeforeRetry(tryCount);
                tryCount++;
                continue;

            } catch (IOException e) {
                lastException = e;

                // Проверяем, можно ли повторить эту ошибку
                if (!isRetryableException(e) || tryCount == MAX_RETRIES - 1) {
                    Logger.e(MyApplication.getContext(), TAG,
                            String.format("Неустранимая ошибка после %d попыток: %s", tryCount + 1, e.getMessage()));
                    throw e;
                }

                Logger.w(MyApplication.getContext(), TAG,
                        String.format("Ошибка (попытка %d/%d): %s, повтор через %d мс",
                                tryCount + 1, MAX_RETRIES, e.getMessage(), getDelayMs(tryCount)));

                waitBeforeRetry(tryCount);
                tryCount++;
                continue;
            }

            // Увеличиваем счетчик для HTTP ошибок
            tryCount++;
            if (tryCount < MAX_RETRIES) {
                waitBeforeRetry(tryCount - 1);
            }
        }

        // Все попытки исчерпаны
        if (lastException != null) {
            throw lastException;
        }
        throw new IOException("Не удалось выполнить запрос после " + MAX_RETRIES + " попыток");
    }

    private long getDelayMs(int attempt) {
        // Экспоненциальная задержка: 1, 2, 4 секунды
        return BASE_DELAY_MS * (1L << attempt);
    }

    private void waitBeforeRetry(int attempt) {
        long delay = getDelayMs(attempt);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.w(MyApplication.getContext(), TAG, "Ожидание прервано");
        }
    }

    private boolean isRetryableException(IOException e) {
        for (Class<?> exceptionClass : RETRYABLE_EXCEPTIONS) {
            if (exceptionClass.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNetworkConnected() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    MyApplication.getContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}