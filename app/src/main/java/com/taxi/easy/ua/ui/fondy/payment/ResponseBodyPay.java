package com.taxi.easy.ua.ui.fondy.payment;

import com.google.gson.annotations.SerializedName;

public class ResponseBodyPay {
    @SerializedName("response")
    private SuccessResponseDataPay successResponse;

    @SerializedName("error_response")
    private ErrorDataPay errorResponse;

    public SuccessResponseDataPay getSuccessResponse() {
        return successResponse;
    }

    public ErrorDataPay getErrorResponse() {
        return errorResponse;
    }

    @Override
    public String toString() {
        return "ResponseBodyRev{" +
                "successResponse=" + successResponse +
                ", errorResponse=" + errorResponse +
                '}';
    }
}
