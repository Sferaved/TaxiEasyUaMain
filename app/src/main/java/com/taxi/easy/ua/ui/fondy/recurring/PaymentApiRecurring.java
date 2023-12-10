package com.taxi.easy.ua.ui.fondy.recurring;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface PaymentApiRecurring {
    @Headers("Content-Type: application/json")
    @POST("recurring")
    Call<ApiResponseRecurring<SuccessResponseDataRecurring>> makeRecurring(@Body StatusRequestRecurring paymentRequest);
}

