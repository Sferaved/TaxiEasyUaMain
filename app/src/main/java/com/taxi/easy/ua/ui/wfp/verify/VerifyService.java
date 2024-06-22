package com.taxi.easy.ua.ui.wfp.verify;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface VerifyService {
    @GET("/wfp/verify/{application}/{city}/{orderReference}/{clientEmail}/{clientPhone}/{language}")
    Call<String> verify(
            @Path("application") String application,
            @Path("city") String city,
            @Path("orderReference") String orderReference,
            @Path("clientEmail") String clientEmail,
            @Path("clientPhone") String clientPhone,
            @Path("language") String language
    );
}
