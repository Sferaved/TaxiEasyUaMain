package com.taxi.easy.ua.ui.fondy.token_pay;

import com.google.gson.annotations.SerializedName;

public class ResponseBodyToken {
    @SerializedName("response")
    private SuccessResponseDataToken successResponse;
    public SuccessResponseDataToken getSuccessResponse() {
        return successResponse;
    }
    @Override
    public String toString() {
        return "ResponseBodyRev{" +
                "successResponse=" + successResponse +
                '}';
    }
}
