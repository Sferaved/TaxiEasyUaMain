package com.taxi.easy.ua.ui.visicom.visicom_search;

import com.taxi.easy.ua.ui.visicom.visicom_search.resp.GeocodeResponse;

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
