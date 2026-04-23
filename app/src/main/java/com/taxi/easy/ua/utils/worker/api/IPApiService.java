package com.taxi.easy.ua.utils.worker.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface IPApiService {
    @GET("/save-ip-with-email/{page}/{email}")
    Call<Void> saveIPWithEmail(
            @Path("page") String page,
            @Path("email") String email
    );
}