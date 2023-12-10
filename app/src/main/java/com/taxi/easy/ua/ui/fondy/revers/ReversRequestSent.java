package com.taxi.easy.ua.ui.fondy.revers;

import com.google.gson.annotations.SerializedName;

public class ReversRequestSent {
    @SerializedName("request")
    private ReversRequestData request;

    public ReversRequestSent(ReversRequestData request) {
        this.request = request;
    }

    public ReversRequestData getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "{" +
                "request=" + request +
                '}';
    }
}
