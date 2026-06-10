package com.taxi.easy.ua.ui.wfp.googlepay;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GooglePayConfigService {

    @GET("/wfp/googlePayConfig/{application}/{city}")
    Call<GooglePayConfigResponse> getConfig(
            @Path("application") String application,
            @Path("city") String city
    );
}
