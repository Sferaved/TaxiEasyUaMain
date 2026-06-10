package com.taxi.easy.ua.ui.wfp.googlepay;

import com.google.gson.annotations.SerializedName;

public class GooglePayChargeRequest {

    @SerializedName("application")
    private final String application;

    @SerializedName("city")
    private final String city;

    @SerializedName("orderReference")
    private final String orderReference;

    @SerializedName("amount")
    private final int amount;

    @SerializedName("productName")
    private final String productName;

    @SerializedName("clientEmail")
    private final String clientEmail;

    @SerializedName("clientPhone")
    private final String clientPhone;

    @SerializedName("paymentDataJson")
    private final String paymentDataJson;

    public GooglePayChargeRequest(
            String application,
            String city,
            String orderReference,
            int amount,
            String productName,
            String clientEmail,
            String clientPhone,
            String paymentDataJson
    ) {
        this.application = application;
        this.city = city;
        this.orderReference = orderReference;
        this.amount = amount;
        this.productName = productName;
        this.clientEmail = clientEmail;
        this.clientPhone = clientPhone;
        this.paymentDataJson = paymentDataJson;
    }
}
