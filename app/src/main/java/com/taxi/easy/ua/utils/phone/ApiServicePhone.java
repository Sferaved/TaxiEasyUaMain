package com.taxi.easy.ua.utils.phone;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiServicePhone {
    @GET("userPhoneReturn/{email}")
    Call<UserPhoneResponse> getUserPhone(@Path("email") String email);
}
