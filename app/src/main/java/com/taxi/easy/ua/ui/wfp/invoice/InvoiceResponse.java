package com.taxi.easy.ua.ui.wfp.invoice;

import com.google.gson.annotations.SerializedName;

public class InvoiceResponse {
    @SerializedName("invoiceUrl")
    private String invoiceUrl;

    @SerializedName("reason")
    private String reason;

    @SerializedName("reasonCode")
    private int reasonCode;

    @SerializedName("qrCode")
    private String qrCode;

    // геттеры и сеттеры

    public String getInvoiceUrl() {
        return invoiceUrl;
    }

    public String getReason() {
        return reason;
    }

    public int getReasonCode() {
        return reasonCode;
    }

    public String getQrCode() {
        return qrCode;
    }
}

