package com.taxi.easy.ua.utils.hold;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface APIHoldService {
    @GET("/wfp/verifyHold/{uid}")
    Call<HoldResponse> verifyHold(
            @Path("uid") String uid
    );

    @GET("/wfp/deleteInvoice/{orderReference}")
    Call<HoldResponse> deleteInvoice(
            @Path("orderReference") String orderReference
    );
}
