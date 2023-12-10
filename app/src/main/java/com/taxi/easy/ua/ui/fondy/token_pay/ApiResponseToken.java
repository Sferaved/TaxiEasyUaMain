package com.taxi.easy.ua.ui.fondy.token_pay;

import com.google.gson.annotations.SerializedName;

public class ApiResponseToken<T> {
    @SerializedName("response")
    private T response;

    public T getResponse() {
        return response;
    }
}



