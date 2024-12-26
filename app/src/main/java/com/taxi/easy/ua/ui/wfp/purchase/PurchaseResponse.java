package com.taxi.easy.ua.ui.wfp.purchase;

import com.google.gson.annotations.SerializedName;

public class PurchaseResponse {
    @SerializedName("merchantAccount")
    private String merchantAccount;

    @SerializedName("orderReference")
    private String orderReference;

    @SerializedName("merchantSignature")
    private String merchantSignature;

    @SerializedName("amount")
    private double amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("authCode")
    private String authCode;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

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

    @SerializedName("recToken")
    private String recToken;

    @SerializedName("transactionStatus")
    private String transactionStatus;

    @SerializedName("reason")
    private String reason;

    @SerializedName("reasonCode")
    private String reasonCode;

    @SerializedName("fee")
    private double fee;

    @SerializedName("paymentSystem")
    private String paymentSystem;

    // геттеры и сеттеры

    public String getMerchantAccount() {
        return merchantAccount;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public String getMerchantSignature() {
        return merchantSignature;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAuthCode() {
        return authCode;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
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

    public String getRecToken() {
        return recToken;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public String getReason() {
        return reason;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public double getFee() {
        return fee;
    }

    public String getPaymentSystem() {
        return paymentSystem;
    }

    @Override
    public String toString() {
        return "PurchaseResponse{" +
                "merchantAccount='" + merchantAccount + '\'' +
                ", orderReference='" + orderReference + '\'' +
                ", merchantSignature='" + merchantSignature + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", authCode='" + authCode + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", createdDate=" + createdDate +
                ", processingDate=" + processingDate +
                ", cardPan='" + cardPan + '\'' +
                ", cardType='" + cardType + '\'' +
                ", issuerBankCountry='" + issuerBankCountry + '\'' +
                ", issuerBankName='" + issuerBankName + '\'' +
                ", recToken='" + recToken + '\'' +
                ", transactionStatus='" + transactionStatus + '\'' +
                ", reason='" + reason + '\'' +
                ", reasonCode='" + reasonCode + '\'' +
                ", fee=" + fee +
                ", paymentSystem='" + paymentSystem + '\'' +
                '}';
    }
}

