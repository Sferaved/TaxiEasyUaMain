package com.taxi.easy.ua.ui.fondy.status;

import com.google.gson.annotations.SerializedName;

public class StatusResponse {
    @SerializedName("response")
    private StatusResponseData response;

    public StatusResponseData getResponse() {
        return response;
    }
}
