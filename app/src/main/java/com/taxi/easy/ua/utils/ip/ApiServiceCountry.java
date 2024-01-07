package com.taxi.easy.ua.utils.ip;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiServiceCountry {

    @GET("ip/countryName/{ipAddress}")
    Call<CountryResponse> getCountryByIP(@Path("ipAddress") String ipAddress);
}
