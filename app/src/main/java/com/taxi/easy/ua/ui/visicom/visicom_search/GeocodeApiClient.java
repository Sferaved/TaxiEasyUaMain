package com.taxi.easy.ua.ui.visicom.visicom_search;

import retrofit2.Retrofit;

public class GeocodeApiClient {

    private final GeocodeApiService apiService;

    public GeocodeApiClient() {
        Retrofit retrofit = RetrofitClientInstance.getRetrofitInstance();
        apiService = retrofit.create(GeocodeApiService.class);
    }

    public GeocodeApiService getApiService() {
        return apiService;
    }
}
