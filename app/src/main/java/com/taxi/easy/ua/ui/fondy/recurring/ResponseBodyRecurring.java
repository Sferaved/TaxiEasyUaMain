package com.taxi.easy.ua.ui.fondy.recurring;

import com.google.gson.annotations.SerializedName;

public class ResponseBodyRecurring {
    @SerializedName("response")
    private SuccessResponseDataRecurring successResponse;
    public SuccessResponseDataRecurring getSuccessResponse() {
        return successResponse;
    }
    @Override
    public String toString() {
        return "ResponseBodyRev{" +
                "successResponse=" + successResponse +
                '}';
    }
}
