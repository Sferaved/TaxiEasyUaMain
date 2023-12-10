package com.taxi.easy.ua.ui.mono.payment;

import com.google.gson.annotations.SerializedName;

public class ResponsePayMono {
    private String invoiceId;

    @SerializedName("pageUrl")
    private String pageUrl;

    // Геттеры и сеттеры для обоих полей

    public String getInvoiceId() {
        return invoiceId;
    }
    public String getPageUrl() {
        return pageUrl;
    }

    @Override
    public String toString() {
        return "ResponseStatusMono{" +
                "invoiceId='" + invoiceId + '\'' +
                ", pageUrl='" + pageUrl + '\'' +
                '}';
    }
}
