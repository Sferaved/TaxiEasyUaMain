package com.taxi.easy.ua.ui.fondy.recurring;

import com.google.gson.annotations.SerializedName;
import com.taxi.easy.ua.ui.fondy.SignatureGenerator;

import java.util.Map;
import java.util.TreeMap;

public class RequestDataRecurring {
    @SerializedName("order_id")
    private String order_id; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("order_desc")
    private String order_desc; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("currency")
    private String currency; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("amount")
    private String amount; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("rectoken")
    private String rectoken; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("merchant_id")
    private String merchant_id; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("preauth")
    private String preauth; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("signature")
    private String signature; // Это поле не нужно аннотировать, так как имя совпадает

    public RequestDataRecurring(String orderId, String orderDescription, String amount, String merchantId, String merchantPassword, String rectoken) {
        this.order_id = orderId; // Используйте поле order_id, а не orderId
        this.order_desc = orderDescription; // Используйте поле order_desc, а не orderDescription
        this.currency = "UAH"; // Установите значение валюты
        this.amount = amount;
        this.rectoken = rectoken;
        this.merchant_id = merchantId;
        this.preauth = "Y";
        this.signature = generateSignature(merchantPassword, createParameterMap());
    }

    private Map<String, String> createParameterMap() {
        Map<String, String> params = new TreeMap<>();
        params.put("order_id", order_id);
        params.put("order_desc", order_desc);
        params.put("currency", currency);
        params.put("amount", amount);
        params.put("rectoken", rectoken);
        params.put("merchant_id", merchant_id);
        params.put("preauth", preauth);
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
                ", order_desc='" + order_desc + '\'' +
                ", currency='" + currency + '\'' +
                ", amount='" + amount + '\'' +
                ", signature='" + signature + '\'' +
                ", rectoken='" + rectoken + '\'' +
                ", merchant_id='" + merchant_id + '\'' +
                '}';
    }
}
