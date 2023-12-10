package com.taxi.easy.ua.ui.open_map.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("reverseAddress/{latitude}/{longitude}")
    Call<ApiResponse> reverseAddress(
            @Path("latitude") double latitude,
            @Path("longitude") double longitude
    );
}
