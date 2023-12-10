package com.taxi.easy.ua.ui.fondy.status;

import com.google.gson.annotations.SerializedName;

public class SuccessfulResponseData {
    @SerializedName("rrn")
    private String rrn;

    @SerializedName("masked_card")
    private String maskedCard;

    @SerializedName("sender_cell_phone")
    private String senderCellPhone;

    @SerializedName("response_signature_string")
    private String responseSignatureString;

    @SerializedName("response_status")
    private String responseStatus;

    @SerializedName("currency")
    private String currency;

    @SerializedName("fee")
    private String fee;

    @SerializedName("reversal_amount")
    private String reversalAmount;

    @SerializedName("settlement_amount")
    private String settlementAmount;

    @SerializedName("actual_amount")
    private String actualAmount;

    @SerializedName("order_status")
    private String orderStatus;

    @SerializedName("response_description")
    private String responseDescription;

    @SerializedName("order_time")
    private String orderTime;

    @SerializedName("actual_currency")
    private String actualCurrency;

    @SerializedName("order_id")
    private String orderId;

    @SerializedName("tran_type")
    private String tranType;

    @SerializedName("eci")
    private String eci;

    @SerializedName("settlement_date")
    private String settlementDate;

    @SerializedName("payment_system")
    private String paymentSystem;

    @SerializedName("approval_code")
    private String approvalCode;

    @SerializedName("merchant_id")
    private String merchantId;

    @SerializedName("settlement_currency")
    private String settlementCurrency;

    @SerializedName("payment_id")
    private String paymentId;

    @SerializedName("card_bin")
    private String cardBin;

    @SerializedName("response_code")
    private String responseCode;

    @SerializedName("card_type")
    private String cardType;

    @SerializedName("amount")
    private String amount;

    @SerializedName("sender_email")
    private String senderEmail;

    @SerializedName("signature")
    private String signature;
    @SerializedName("error_message")
    private String error_message;

    @SerializedName("error_code")
    private String error_code;

    public String getError_message() {
        return error_message;
    }

    public String getError_code() {
        return error_code;
    }
    public String getRrn() {
        return rrn;
    }

    public String getMaskedCard() {
        return maskedCard;
    }

    public String getSenderCellPhone() {
        return senderCellPhone;
    }

    public String getResponseSignatureString() {
        return responseSignatureString;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFee() {
        return fee;
    }

    public String getReversalAmount() {
        return reversalAmount;
    }

    public String getSettlementAmount() {
        return settlementAmount;
    }

    public String getActualAmount() {
        return actualAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public String getActualCurrency() {
        return actualCurrency;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getTranType() {
        return tranType;
    }

    public String getEci() {
        return eci;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public String getPaymentSystem() {
        return paymentSystem;
    }

    public String getApprovalCode() {
        return approvalCode;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getSettlementCurrency() {
        return settlementCurrency;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getCardBin() {
        return cardBin;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getCardType() {
        return cardType;
    }

    public String getAmount() {
        return amount;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSignature() {
        return signature;
    }

    public String getProductId() {
        return productId;
    }

    @SerializedName("product_id")
    private String productId;

    @Override
    public String toString() {
        return "SuccessfulResponseData{" +
                "rrn='" + rrn + '\'' +
                ", maskedCard='" + maskedCard + '\'' +
                ", senderCellPhone='" + senderCellPhone + '\'' +
                ", responseSignatureString='" + responseSignatureString + '\'' +
                ", responseStatus='" + responseStatus + '\'' +
                ", currency='" + currency + '\'' +
                ", fee='" + fee + '\'' +
                ", reversalAmount='" + reversalAmount + '\'' +
                ", settlementAmount='" + settlementAmount + '\'' +
                ", actualAmount='" + actualAmount + '\'' +
                ", orderStatus='" + orderStatus + '\'' +
                ", responseDescription='" + responseDescription + '\'' +
                ", orderTime='" + orderTime + '\'' +
                ", actualCurrency='" + actualCurrency + '\'' +
                ", orderId='" + orderId + '\'' +
                ", tranType='" + tranType + '\'' +
                ", eci='" + eci + '\'' +
                ", settlementDate='" + settlementDate + '\'' +
                ", paymentSystem='" + paymentSystem + '\'' +
                ", approvalCode='" + approvalCode + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", settlementCurrency='" + settlementCurrency + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", cardBin='" + cardBin + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", cardType='" + cardType + '\'' +
                ", amount='" + amount + '\'' +
                ", senderEmail='" + senderEmail + '\'' +
                ", signature='" + signature + '\'' +
                ", productId='" + productId + '\'' +
                '}';
    }

    // Геттеры и сеттеры для полей, если необходимо
    // ...
}

