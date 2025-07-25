package com.taxi.easy.ua.service;

import com.google.gson.annotations.SerializedName;

public class OrderServiceResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    // Геттеры и сеттеры
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}