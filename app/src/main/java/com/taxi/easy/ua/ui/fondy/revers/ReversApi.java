package com.taxi.easy.ua.ui.fondy.revers;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ReversApi {
    @Headers("Content-Type: application/json")
    @POST("reverse/order_id")
    Call<ApiResponseRev<SuccessResponseDataRevers>> makeRevers(@Body ReversRequestSent reversRequestSent);
}

