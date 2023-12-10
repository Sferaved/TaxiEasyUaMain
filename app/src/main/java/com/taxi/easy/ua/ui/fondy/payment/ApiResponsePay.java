package com.taxi.easy.ua.ui.fondy.payment;

import com.google.gson.annotations.SerializedName;

public class ApiResponsePay<T> {
    @SerializedName("response")
    private T response;

    public T getResponse() {
        return response;
    }
}



