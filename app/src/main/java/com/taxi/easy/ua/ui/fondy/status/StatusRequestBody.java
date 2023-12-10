package com.taxi.easy.ua.ui.fondy.status;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.taxi.easy.ua.ui.fondy.SignatureGenerator;

import java.util.Map;
import java.util.TreeMap;



public class StatusRequestBody {
    @SerializedName("order_id")
    private String order_id;

    @SerializedName("merchant_id")
    private String merchant_id;

    @SerializedName("signature")
    private String signature;

    public StatusRequestBody(String order_id, String merchant_id, String merchantPassword) {
        this.order_id = order_id;
        this.merchant_id = merchant_id;
        this.signature = generateSignature(merchantPassword, createParameterMap());
    }

    public String getOrder_id() {
        return order_id;
    }

    public String getMerchant_id() {
        return merchant_id;
    }

    public String getSignature() {
        return signature;
    }

    private Map<String, String> createParameterMap() {
        Map<String, String> params = new TreeMap<>();
        params.put("order_id", order_id);
        params.put("merchant_id", merchant_id);
        // Добавьте другие параметры, если необходимо

        return params;
    }

    private String generateSignature(String merchantPassword, Map<String, String> params) {
        Log.d("TAG", "generateSignature: " + SignatureGenerator.generateSignature(merchantPassword, params));
        return SignatureGenerator.generateSignature(merchantPassword, params);
    }

    @Override
    public String toString() {
        return "StatusRequestBody{" +
                "order_id='" + order_id + '\'' +
                ", merchant_id='" + merchant_id + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}



