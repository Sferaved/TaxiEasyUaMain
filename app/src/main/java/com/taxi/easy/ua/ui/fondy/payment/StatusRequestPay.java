package com.taxi.easy.ua.ui.fondy.payment;

import com.google.gson.annotations.SerializedName;

public class StatusRequestPay {
    @SerializedName("request")
    private RequestData request;

    public StatusRequestPay(RequestData request) {
        this.request = request;
    }

    public RequestData getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "{" +
                "request=" + request +
                '}';
    }
}
