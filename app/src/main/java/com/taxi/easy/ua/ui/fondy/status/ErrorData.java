package com.taxi.easy.ua.ui.fondy.status;

import com.google.gson.annotations.SerializedName;

public class ErrorData {
    @SerializedName("error_message")
    private String error_message;

    @SerializedName("error_code")
    private String error_code;

    public String getError_message() {
        return error_message;
    }

    public String getError_code() {
        return error_code;
    }
}
