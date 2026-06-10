package com.taxi.easy.ua.ui.wfp.googlepay;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GooglePayChargeService {

    @POST("/wfp/googlePayCharge")
    Call<GooglePayChargeResponse> charge(@Body GooglePayChargeRequest request);
}
