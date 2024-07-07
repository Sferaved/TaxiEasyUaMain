package com.taxi.easy.ua.utils.user;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiServiceUser {
    @GET("android/addUserNoNameApp/{email}/{app}")
    Call<UserResponse> addUserNoName(
            @Path("email") String email,
            @Path("app") String app
    );
}
