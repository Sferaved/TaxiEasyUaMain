package com.taxi.easy.ua.ui.fondy.callback;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CallbackService {
    @GET("/get-card-token/{email}/{pay_system}")
    Call<CallbackResponse> handleCallback(
            @Path("email") String email,
            @Path("pay_system") String pay_system
    );
}
