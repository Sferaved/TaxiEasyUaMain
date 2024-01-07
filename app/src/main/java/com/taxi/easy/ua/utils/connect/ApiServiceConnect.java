package com.taxi.easy.ua.utils.connect;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiServiceConnect {
    @GET("geocode.json")
    Call<Void> testConnection(
            @Query("categories") String categories,
            @Query("text") String text,
            @Query("key") String key
    );
}


