package com.taxi.easy.ua.ui.fondy.payment;

import com.google.gson.annotations.SerializedName;

public class PaymentRequest {
    @SerializedName("request")
    private RequestData request;

    public PaymentRequest(String orderId, String orderDescription, String amount, String merchantId, String merchantPassword, String email) {
        this.request = new RequestData(orderId, orderDescription, amount, merchantId, merchantPassword, email);
    }

    @Override
    public String toString() {
        return "ReversRequest{" +
                "request=" + request +
                '}';
    }
}

