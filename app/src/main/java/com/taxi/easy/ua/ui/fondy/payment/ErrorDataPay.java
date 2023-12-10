package com.taxi.easy.ua.ui.fondy.payment;

import com.google.gson.annotations.SerializedName;

public class ErrorDataPay {
    @SerializedName("response_status")
    private String responseStatus;

    @SerializedName("error_message")
    private String errorMessage;

    @SerializedName("error_code")
    private String errorCode;

    public String getResponseStatus() {
        return responseStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

