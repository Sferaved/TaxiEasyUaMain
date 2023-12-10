package com.taxi.easy.ua.ui.fondy.payment;

import com.google.gson.annotations.SerializedName;
import com.taxi.easy.ua.ui.fondy.status.ErrorData;

public class ErrorResponse {
    @SerializedName("response")
    private ErrorData response;

    public ErrorData getResponse() {
        return response;
    }
}

