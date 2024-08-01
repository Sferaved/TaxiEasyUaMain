package com.taxi.easy.ua.utils.tariff;

import android.content.Context;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TariffInfo {
    private final Context context;

    public TariffInfo(Context context) {
        this.context = context;
    }

    private static final String BASE_URL = "https://m.easy-order-taxi.site/" + MainActivity.api + "/android/";

    public void fetchOrderCostDetails(double startLat, double startLng, double endLat, double endLng, String user, String services, String city, String application) {
        // Создание логгера для логирования запросов



        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Создание клиента OkHttpClient с подключенным логгером
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(loggingInterceptor);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build()) // Подключение клиента OkHttpClient с логгером
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<Tariff>> call = apiService.getOrderCostDetails(startLat, startLng, endLat, endLng, user, services, city, application);

        call.enqueue(new Callback<List<Tariff>>() {
            @Override
            public void onResponse(@NonNull Call<List<Tariff>> call, @NonNull Response<List<Tariff>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        DatabaseHelperTariffs databaseHelperTariffs = new DatabaseHelperTariffs(context);
                        List<Tariff> tariffList = response.body();
                        for (Tariff tariff : tariffList) {
                            String flexibleTariffName = tariff.getFlexibleTariffName();
//                            Log.d("TariffDetails", "Flexible Tariff Name: " + flexibleTariffName);
                            OrderCostDetails orderCostDetails = tariff.getOrderCostDetails();
                            if (orderCostDetails != null) { // Проверка на null
                                String dispatchingOrderUid = orderCostDetails.getDispatchingOrderUid();
                                String orderCost = orderCostDetails.getOrderCost();
                                String addCost = orderCostDetails.getAddCost();
                                String recommendedAddCost = orderCostDetails.getRecommendedAddCost();
                                String currency = orderCostDetails.getCurrency();
                                String discountTrip = orderCostDetails.isDiscountTrip();
                                String canPayBonuses = orderCostDetails.isCanPayBonuses();
                                String canPayCashless = orderCostDetails.isCanPayCashless();

                                // Использование полученных данных...

                                databaseHelperTariffs.insertOrUpdateTariff(
                                        flexibleTariffName,
                                        dispatchingOrderUid,
                                        orderCost,
                                        addCost,
                                        recommendedAddCost,
                                        currency,
                                        discountTrip,
                                        canPayBonuses,
                                        canPayCashless
                                );
                           }
                        }

                        try {
                        } finally {
                            databaseHelperTariffs.close();
                        }
                    } catch (Exception ignored) {}
//                    VisicomFragment.readTariffInfo(context);
                } else {
                    // Обработка ошибки
                }

            }

            @Override
            public void onFailure(@NonNull Call<List<Tariff>> call, @NonNull Throwable t) {
                // Обработка ошибки
            }
        });


    }

}

