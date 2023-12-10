package com.taxi.easy.ua.ui.mono.cancel;

import com.google.gson.annotations.SerializedName;

public class ResponseCancelMono {
    @SerializedName("status")
    private String status;
    @SerializedName("createdDate")
    private String createdDate;
    @SerializedName("modifiedDate")
    private String modifiedDate;
    @SerializedName("errCode")
    private String errCode;

    public String getErrCode() {
        return errCode;
    }

    public String getErrText() {
        return errText;
    }

    @SerializedName("errText")
    private String errText;

    public String getStatus() {
        return status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }
}
