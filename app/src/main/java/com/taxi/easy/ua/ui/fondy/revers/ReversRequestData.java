package com.taxi.easy.ua.ui.fondy.revers;

import com.google.gson.annotations.SerializedName;
import com.taxi.easy.ua.ui.fondy.SignatureGenerator;

import java.util.Map;
import java.util.TreeMap;

public class ReversRequestData {
    @SerializedName("order_id")
    private String order_id; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("merchant_id")
    private String merchant_id; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("signature")
    private String signature; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("amount")
    private String amount; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("currency")
    private String currency; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("comment")
    private String comment; // Имя поля должно соответствовать JSON-запросу

    public ReversRequestData(String orderId, String comment, String amount, String merchantId, String merchantPassword) {
        this.order_id = orderId;
        this.merchant_id = merchantId;
        this.amount = amount;
        this.currency = "UAH";
        this.comment = comment;
        this.signature = generateSignature(merchantPassword, createParameterMap());
    }

    private Map<String, String> createParameterMap() {
        Map<String, String> params = new TreeMap<>();
        params.put("order_id", order_id);
        params.put("merchant_id", merchant_id);
        params.put("amount", amount);
        params.put("currency", currency);
        params.put("comment", comment);
        // Добавьте другие параметры, если необходимо

        return params;
    }

    private String generateSignature(String merchantPassword, Map<String, String> params) {
        return SignatureGenerator.generateSignature(merchantPassword, params);
    }

    @Override
    public String toString() {
        return "ReversRequestData{" +
                "order_id='" + order_id + '\'' +
                ", merchant_id='" + merchant_id + '\'' +
                ", signature='" + signature + '\'' +
                ", amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
