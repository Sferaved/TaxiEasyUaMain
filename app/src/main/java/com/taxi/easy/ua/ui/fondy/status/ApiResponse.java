package com.taxi.easy.ua.ui.fondy.status;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("response")
    private T response;

    public T getResponse() {
        return response;
    }
}



