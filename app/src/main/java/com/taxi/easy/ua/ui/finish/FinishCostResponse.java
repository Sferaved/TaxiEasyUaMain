package com.taxi.easy.ua.ui.finish;

import com.google.gson.annotations.SerializedName;

public class FinishCostResponse {
    @SerializedName("result")
    private String result;

    @SerializedName("message")
    private String message;

    @SerializedName("finish_cost")
    private double finishCost;

    @SerializedName("order_id")
    private int orderId;

    // Constructor
    public FinishCostResponse() {}

    // Getters and Setters
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getFinishCost() {
        return finishCost;
    }

    public void setFinishCost(double finishCost) {
        this.finishCost = finishCost;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return "FinishCostResponse{" +
                "result='" + result + '\'' +
                ", message='" + message + '\'' +
                ", finishCost=" + finishCost +
                ", orderId=" + orderId +
                '}';
    }
}