package com.taxi.easy.ua.utils.fcm.token_send;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiServiceToken {
    @POST("android_token/store/{email}/{app}/{token}/")
    Call<Void> sendToken(
            @Path("email") String email,
            @Path("app") String app,
            @Path("token") String token
    );
}