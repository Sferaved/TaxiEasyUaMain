package com.taxi.easy.ua.ui.fondy.status;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("response")
    private ErrorData response;

    public ErrorData getResponse() {
        return response;
    }
}

