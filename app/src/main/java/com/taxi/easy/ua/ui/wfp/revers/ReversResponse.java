package com.taxi.easy.ua.ui.wfp.revers;

import com.google.gson.annotations.SerializedName;

public class ReversResponse {
    @SerializedName("orderReference")
    private String orderReference;

    @SerializedName("reasonCode")
    private int reasonCode;
    @SerializedName("reason")
    private String reason;
    @SerializedName("transactionStatus")
    private String transactionStatus;
    @SerializedName("merchantAccount")
    private String merchantAccount;


    // геттеры и сеттеры

    public String getMerchantAccount() {
        return merchantAccount;
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


    public String getTransactionStatus() {
        return transactionStatus;
    }


}
