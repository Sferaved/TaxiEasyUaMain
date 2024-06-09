package com.taxi.easy.ua.ui.wfp.checkStatus;

import com.google.gson.annotations.SerializedName;

public class StatusResponse {
    @SerializedName("merchantAccount")
    private String merchantAccount;

    @SerializedName("reason")
    private String reason;

    @SerializedName("reasonCode")
    private int reasonCode;

    @SerializedName("orderReference")
    private String orderReference;

    @SerializedName("amount")
    private int amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("authCode")
    private String authCode;

    @SerializedName("createdDate")
    private long createdDate;

    @SerializedName("processingDate")
    private long processingDate;

    @SerializedName("cardPan")
    private String cardPan;

    @SerializedName("cardType")
    private String cardType;

    @SerializedName("issuerBankCountry")
    private String issuerBankCountry;

    @SerializedName("issuerBankName")
    private String issuerBankName;

    @SerializedName("transactionStatus")
    private String transactionStatus;

    @SerializedName("refundAmount")
    private int refundAmount;

    @SerializedName("settlementDate")
    private String settlementDate;

    @SerializedName("settlementAmount")
    private int settlementAmount;

    @SerializedName("fee")
    private double fee;

    @SerializedName("merchantSignature")
    private String merchantSignature;

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

    public int getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAuthCode() {
        return authCode;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public long getProcessingDate() {
        return processingDate;
    }

    public String getCardPan() {
        return cardPan;
    }

    public String getCardType() {
        return cardType;
    }

    public String getIssuerBankCountry() {
        return issuerBankCountry;
    }

    public String getIssuerBankName() {
        return issuerBankName;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public int getRefundAmount() {
        return refundAmount;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public int getSettlementAmount() {
        return settlementAmount;
    }

    public double getFee() {
        return fee;
    }

    public String getMerchantSignature() {
        return merchantSignature;
    }
}

