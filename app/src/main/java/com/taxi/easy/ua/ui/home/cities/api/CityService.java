package com.taxi.easy.ua.ui.home.cities.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CityService {
    @GET("city/maxPayValueApp/{city}/{app}")
    Call<CityResponse> getMaxPayValues(
            @Path("city") String city,
            @Path("app") String app
            );
    @GET("city/merchantFondy/{city}")
    Call<CityResponseMerchantFondy> getMerchantFondy(@Path("city") String city);

    @GET("lastAddressUser/{email}/{city}/{app}")
    Call<CityLastAddressResponse> lastAddressUser(
            @Path("email") String email,
            @Path("city") String city,
            @Path("app") String app
    );
}
