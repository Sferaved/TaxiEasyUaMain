package com.taxi.easy.ua.ui.open_map.mapbox;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MapboxService {

    @GET("geocoding/v5/mapbox.places/{address}.json")
    Call<MapboxResponse> getLocation(
            @Path("address") String address,
//            @Query("country") String country, // Добавляем параметр country
            @Query("access_token") String accessToken
    );
}
