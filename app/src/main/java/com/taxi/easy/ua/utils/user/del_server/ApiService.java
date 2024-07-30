package com.taxi.easy.ua.utils.user.del_server;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("users/destroyEmail/{email}")
    Call<ServerResponse> destroyEmail(@Path("email") String email);
}

