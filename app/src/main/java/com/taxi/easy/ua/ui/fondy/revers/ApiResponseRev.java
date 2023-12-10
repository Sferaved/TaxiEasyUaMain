package com.taxi.easy.ua.ui.fondy.revers;

import com.google.gson.annotations.SerializedName;

public class ApiResponseRev<T> {
    @SerializedName("response")
    private T response;

    public T getResponse() {
        return response;
    }
}



