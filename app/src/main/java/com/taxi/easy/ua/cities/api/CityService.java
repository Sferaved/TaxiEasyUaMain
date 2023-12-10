package com.taxi.easy.ua.cities.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CityService {
    @GET("city/maxPayValue/{city}")
    Call<CityResponse> getMaxPayValues(@Path("city") String city);
    @GET("city/merchantFondy/{city}")
    Call<CityResponseMerchantFondy> getMerchantFondy(@Path("city") String city);
}

