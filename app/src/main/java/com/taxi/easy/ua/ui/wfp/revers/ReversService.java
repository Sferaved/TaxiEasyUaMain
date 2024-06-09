package com.taxi.easy.ua.ui.wfp.revers;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ReversService {
    @GET("/wfp/refund/{application}/{city}/{orderReference}/{amount}")
    Call<ReversResponse> checkStatus(
            @Path("application") String application,
            @Path("city") String city,
            @Path("orderReference") String orderReference,
            @Path("amount") String amount
    );
}
