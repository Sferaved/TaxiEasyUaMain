package com.taxi.easy.ua.ui.wfp.googlepay;

import com.google.gson.annotations.SerializedName;

public class GooglePayChargeResponse {

    @SerializedName("transactionStatus")
    private String transactionStatus;

    @SerializedName("reason")
    private String reason;

    @SerializedName("reasonCode")
    private int reasonCode;

    @SerializedName("orderReference")
    private String orderReference;

    @SerializedName("recToken")
    private String recToken;

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public String getReason() {
        return reason;
    }

    public int getReasonCode() {
        return reasonCode;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public String getRecToken() {
        return recToken;
    }
}
