package com.taxi.easy.ua.ui.visicom.visicom_search.key_visicom;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

//    private static final String BASE_URL = "https://m.easy-order-taxi.site/";
    private static final String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final ApiService apiService = retrofit.create(ApiService.class);

    public static void getVisicomKeyInfo(Callback<ApiResponse> callback, String appName) {
        Call<ApiResponse> call = apiService.getVisicomKeyInfo(appName);
        call.enqueue(callback);
    }
}
