package com.taxi.easy.ua.ui.fondy.status;

import com.google.gson.annotations.SerializedName;

public class StatusRequest {
    @SerializedName("request")
    private StatusRequestBody request;

    public StatusRequest(StatusRequestBody request) {
        this.request = request;
    }

    public StatusRequestBody getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "{" +
                "request=" + request +
                '}';
    }
}
