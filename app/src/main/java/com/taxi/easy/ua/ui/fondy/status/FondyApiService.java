package com.taxi.easy.ua.ui.fondy.status;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FondyApiService {
    @POST("status/order_id")
    Call<ApiResponse<SuccessfulResponseData>> checkOrderStatus(
            @Body StatusRequest statusRequest
    );
}

