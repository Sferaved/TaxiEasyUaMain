package com.taxi.easy.ua.ui.wfp.checkStatus;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface StatusService {
    @GET("/wfp/checkStatus/{application}/{city}/{orderReference}")
    Call<StatusResponse> checkStatus(
            @Path("application") String application,
            @Path("city") String city,
            @Path("orderReference") String orderReference
    );
}

