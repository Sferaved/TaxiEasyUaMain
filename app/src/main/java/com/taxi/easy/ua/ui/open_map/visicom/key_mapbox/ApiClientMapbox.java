package com.taxi.easy.ua.ui.open_map.visicom.key_mapbox;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientMapbox {

    private static final String BASE_URL = "https://m.easy-order-taxi.site/";

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static ApiServiceMapbox apiService = retrofit.create(ApiServiceMapbox.class);

    public static void getMapboxKeyInfo(Callback<ApiResponseMapbox> callback, String appName) {
        Call<ApiResponseMapbox> call = apiService.getMaxboxKeyInfo(appName);
        call.enqueue(callback);
    }
}
