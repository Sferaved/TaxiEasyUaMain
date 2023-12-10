package com.taxi.easy.ua.ui.payment_system;

import retrofit2.Call;
import retrofit2.http.GET;

public interface PayApi {
    @GET("/android_set/getPaySystem")
    Call<ResponsePaySystem> getPaySystem();
}
