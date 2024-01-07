package com.taxi.easy.ua.ui.open_map.visicom.key_mapbox;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiServiceMapbox {

    @GET("maxBoxKeyInfo/{appName}")
    Call<ApiResponseMapbox> getMaxboxKeyInfo(
            @Path("appName") String appName
    );
}
