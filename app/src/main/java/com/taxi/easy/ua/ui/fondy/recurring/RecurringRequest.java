package com.taxi.easy.ua.ui.fondy.recurring;

import com.google.gson.annotations.SerializedName;

public class RecurringRequest {
    @SerializedName("request")
    private RequestDataRecurring request;

    public RecurringRequest(String orderId, String orderDescription, String amount, String merchantId, String merchantPassword, String rectoken) {
        this.request = new RequestDataRecurring(orderId, orderDescription, amount, merchantId, merchantPassword, rectoken);
    }

    @Override
    public String toString() {
        return "ReversRequest{" +
                "request=" + request +
                '}';
    }
}

