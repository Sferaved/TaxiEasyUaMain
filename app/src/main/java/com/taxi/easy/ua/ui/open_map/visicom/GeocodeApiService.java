package com.taxi.easy.ua.ui.open_map.visicom;

import com.taxi.easy.ua.ui.open_map.visicom.resp.GeocodeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeocodeApiService {

    @GET("geocode.json")
    Call<GeocodeResponse> getGeocode(
            @Query("categories") String categories,
            @Query("text") String searchText,
            @Query("key") String apiKey
    );
}
