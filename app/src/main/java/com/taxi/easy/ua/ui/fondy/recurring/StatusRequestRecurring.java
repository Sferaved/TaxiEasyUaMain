package com.taxi.easy.ua.ui.fondy.recurring;

import com.google.gson.annotations.SerializedName;

public class StatusRequestRecurring {
    @SerializedName("request")
    private RequestDataRecurring request;

    public StatusRequestRecurring(RequestDataRecurring request) {
        this.request = request;
    }

    public RequestDataRecurring getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "{" +
                "request=" + request +
                '}';
    }
}
