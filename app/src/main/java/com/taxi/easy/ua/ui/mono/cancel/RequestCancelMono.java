package com.taxi.easy.ua.ui.mono.cancel;

import com.google.gson.annotations.SerializedName;
public class RequestCancelMono {
    @SerializedName("invoiceId")
    private String invoiceId;

    @SerializedName("extRef")
    private String extRef;
    @SerializedName("amount")
    private int amount;

    public RequestCancelMono(
            String invoiceId,
            String extRef,
            int amount
    ) {
        this.invoiceId = invoiceId;
        this.extRef = extRef;
        this.amount = amount;
    }
}
