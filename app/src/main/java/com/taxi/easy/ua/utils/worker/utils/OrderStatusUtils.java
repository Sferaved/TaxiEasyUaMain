package com.taxi.easy.ua.utils.worker.utils;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.CITY_INFO;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.service.AutoOrderResponse;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.OrderStatusWorker;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderStatusUtils {
    private static final String TAG = "OrderStatusUtils";

    public static boolean checkOrders(Context context, OrderStatusWorker worker) throws Exception {
        Logger.d(context, TAG, "===> checkOrders() вызван");

        try {
            // Получаем API и Email
            List<String> cityInfo = logCursor(CITY_INFO, context);
            Logger.d(context, TAG, "logCursor CITY_INFO: " + cityInfo);

            List<String> userInfo = logCursor(MainActivity.TABLE_USER_INFO, context);
            Logger.d(context, TAG, "logCursor TABLE_USER_INFO: " + userInfo);

            String api = cityInfo.size() > 2 ? cityInfo.get(2) : "unknown_api";
            String email = userInfo.size() > 3 ? userInfo.get(3) : "unknown_email";

            Logger.d(context, TAG, "Полученные значения -> API: " + api + ", Email: " + email);

            // Формируем URL
            String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            String url = baseUrl + api + "/android/searchAutoOrderServiceAll/" + email + "/" + context.getString(R.string.application) + "/yes_mes";

            Logger.d(context, TAG, "Сформированный URL для запроса: " + url);

            if (worker.isStopped()) {
                Logger.e(context, TAG, "Worker остановлен до выполнения запроса");
                return false;
            }

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};
            final Exception[] requestException = {null};

            Call<AutoOrderResponse> call = ApiClient.getApiService().searchAutoOrderServiceAll(url);
            Logger.d(context, TAG, "Выполняется запрос searchAutoOrderServiceAll...");

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<AutoOrderResponse> call, @NonNull Response<AutoOrderResponse> response) {
                    Logger.d(context, TAG, "Ответ от сервера получен. Код: " + response.code());
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            AutoOrderResponse result = response.body();
                            int totalOrders = result.getTotal_orders();
                            Logger.d(context, TAG, "Общее количество заказов: " + result.getTotal_orders());

                            if (result.getOrders() != null) {
                                for (AutoOrderResponse.OrderItem item : result.getOrders()) {
                                    Logger.d(context, TAG, "Номер заказа: " + item.getNumber() + ", UID: " + item.getDispatching_order_uid());
                                }
                            }
                            if (totalOrders == 0) {
                                Logger.d(context, TAG, "Заказов нет — останавливаем дальнейший опрос");
                                success[0] = false;  // Важно: возвращаем false, чтобы не планировать следующий запуск
                            } else {
                                success[0] = true;   // Есть заказы — продолжаем опрос
                            }
                        } else {
                            Logger.e(context, TAG, "Ответ не успешный: " + response.code() + ", " + response.message());
                            if (response.errorBody() != null) {
                                try {
                                    Logger.e(context, TAG, "Тело ошибки: " + response.errorBody().string());
                                } catch (Exception e) {
                                    Logger.e(context, TAG, "Ошибка чтения тела ошибки: " + e.toString());
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.e(context, TAG, "Ошибка при обработке ответа: " + e.toString());
                        requestException[0] = e;
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AutoOrderResponse> call, @NonNull Throwable t) {
                    Logger.e(context, TAG, "Ошибка сети или запроса: " + t.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(t);
                    requestException[0] = new Exception("Network error", t);
                    latch.countDown();
                }
            });

            Logger.d(context, TAG, "Ожидание завершения запроса (до 15 сек)...");
            boolean completed = latch.await(15, TimeUnit.SECONDS);

            if (!completed) {
                Logger.e(context, TAG, "Таймаут: запрос не завершен за 15 секунд");
                throw new Exception("Request timed out");
            }

            if (requestException[0] != null) {
                throw requestException[0];
            }

            Logger.d(context, TAG, "checkOrders завершен успешно, результат: " + success[0]);
            return success[0];
        } catch (ParseException e) {
            Logger.e(context, TAG, "ParseException: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static List<String> logCursor(String table, Context context) {
        Logger.d(context, TAG, "Чтение таблицы: " + table);
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);

        Logger.d(context, TAG, "Количество строк в таблице " + table + ": " + c.getCount());

        if (c.moveToFirst()) {
            do {
                for (String cn : c.getColumnNames()) {
                    @SuppressLint("Range") String value = c.getString(c.getColumnIndex(cn));
                    list.add(value);
                    Logger.d(context, TAG, "Column: " + cn + " = " + value);
                }
            } while (c.moveToNext());
        }
        c.close();
        database.close();
        return list;
    }
}
