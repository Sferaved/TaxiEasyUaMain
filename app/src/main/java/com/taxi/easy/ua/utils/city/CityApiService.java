package com.taxi.easy.ua.utils.city;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CityApiService {
    @GET("apiTest/android/findCityJson/{lat}/{lon}")
    Call<CityResponse> findCity(@Path("lat") double latitude, @Path("lon") double longitude);
}

