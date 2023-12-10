package com.taxi.easy.ua.ui.fondy.recurring;

import com.google.gson.annotations.SerializedName;

public class ApiResponseRecurring<T> {
    @SerializedName("response")
    private T response;

    public T getResponse() {
        return response;
    }
}



