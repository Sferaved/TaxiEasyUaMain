package com.taxi.easy.ua.ui.visicom.visicom_search.key_visicom;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    @GET("visicomKeyInfo/{appName}")
    Call<ApiResponse> getVisicomKeyInfo(
            @Path("appName") String appName
    );
}
