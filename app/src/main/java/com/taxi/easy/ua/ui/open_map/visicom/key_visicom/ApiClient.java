package com.taxi.easy.ua.ui.open_map.visicom.key_visicom;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://m.easy-order-taxi.site/";

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static ApiService apiService = retrofit.create(ApiService.class);

    public static void getVisicomKeyInfo(Callback<ApiResponse> callback, String appName) {
        Call<ApiResponse> call = apiService.getVisicomKeyInfo(appName);
        call.enqueue(callback);
    }
}
