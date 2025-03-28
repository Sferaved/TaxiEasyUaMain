package com.taxi.easy.ua.utils.hold;

import com.google.gson.annotations.SerializedName;

public class HoldResponse {
    @SerializedName("result")
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
