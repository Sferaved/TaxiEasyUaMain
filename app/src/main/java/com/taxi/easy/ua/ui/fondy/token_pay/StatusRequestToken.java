package com.taxi.easy.ua.ui.fondy.token_pay;

import com.google.gson.annotations.SerializedName;

public class StatusRequestToken {
    @SerializedName("request")
    private RequestDataToken request;

    public StatusRequestToken(RequestDataToken request) {
        this.request = request;
    }

    public RequestDataToken getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "{" +
                "request=" + request +
                '}';
    }
}
