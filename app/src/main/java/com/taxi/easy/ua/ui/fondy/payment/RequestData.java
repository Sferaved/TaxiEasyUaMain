package com.taxi.easy.ua.ui.fondy.payment;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.taxi.easy.ua.ui.fondy.SignatureGenerator;

import java.util.Map;
import java.util.TreeMap;

public class RequestData {
    @SerializedName("order_id")
    private String order_id; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("order_desc")
    private String order_desc; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("currency")
    private String currency; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("amount")
    private String amount; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("signature")
    private String signature; // Это поле не нужно аннотировать, так как имя совпадает
    @SerializedName("merchant_id")
    private String merchant_id; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("preauth")
    private String preauth; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("sender_email")
    private String sender_email; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("required_rectoken")
    private String required_rectoken; // Имя поля должно соответствовать JSON-запросу
    @SerializedName("server_callback_url")
    private String server_callback_url; // Имя поля должно соответствовать JSON-запросу

    public RequestData(
            String orderId,
            String orderDescription,
            String amount,
            String merchantId,
            String merchantPassword,
            String email) {
        this.order_id = orderId; // Используйте поле order_id, а не orderId
        this.order_desc = orderDescription; // Используйте поле order_desc, а не orderDescription
        this.currency = "UAH"; // Установите значение валюты
        this.amount = amount;
        this.merchant_id = merchantId; // Используйте поле merchant_id, а не merchantId
        this.preauth = "Y";
        this.required_rectoken = "Y";
        this.sender_email = email;
        this.server_callback_url = "https://m.easy-order-taxi.site/server-callback";

        this.signature = generateSignature(merchantPassword, createParameterMap());
    }

    private Map<String, String> createParameterMap() {
        Map<String, String> params = new TreeMap<>();
        params.put("order_id", order_id);
        params.put("order_desc", order_desc);
        params.put("currency", currency);
        params.put("amount", amount);
        params.put("preauth", preauth);
        params.put("required_rectoken", required_rectoken);
        params.put("merchant_id", merchant_id);
        params.put("sender_email", sender_email);
        params.put("server_callback_url", server_callback_url);

        return params;
    }

    private String generateSignature(String merchantPassword, Map<String, String> params) {
        return SignatureGenerator.generateSignature(merchantPassword, params);
    }

    @NonNull
    @Override
    public String toString() {
        return "ReversRequestData{" +
                "order_id='" + order_id + '\'' +
                ", order_desc='" + order_desc + '\'' +
                ", currency='" + currency + '\'' +
                ", amount='" + amount + '\'' +
                ", signature='" + signature + '\'' +
                ", preauth='" + preauth + '\'' +
                ", required_rectoken='" + required_rectoken + '\'' +
                ", merchant_id='" + merchant_id + '\'' +
                ", server_callback_url='" + server_callback_url + '\'' +
                '}';
    }
}
