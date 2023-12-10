package com.taxi.easy.ua.ui.payment;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("id")
    private String id;

    @SerializedName("time")
    private long time;

    @SerializedName("description")
    private String description;

    @SerializedName("mcc")
    private int mcc;

    @SerializedName("originalMcc")
    private int originalMcc;

    @SerializedName("hold")
    private boolean hold;

    @SerializedName("amount")
    private double amount;

    @SerializedName("operationAmount")
    private double operationAmount;

    @SerializedName("currencyCode")
    private int currencyCode;

    @SerializedName("commissionRate")
    private double commissionRate;

    @SerializedName("cashbackAmount")
    private double cashbackAmount;

    @SerializedName("balance")
    private double balance;

    @SerializedName("comment")
    private String comment;

    @SerializedName("receiptId")
    private String receiptId;

    @SerializedName("invoiceId")
    private String invoiceId;

    @SerializedName("counterEdrpou")
    private String counterEdrpou;

    @SerializedName("counterIban")
    private String counterIban;

    @SerializedName("counterName")
    private String counterName;

    // Геттеры и сеттеры для каждого поля

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public int getOriginalMcc() {
        return originalMcc;
    }

    public void setOriginalMcc(int originalMcc) {
        this.originalMcc = originalMcc;
    }

    public boolean isHold() {
        return hold;
    }

    public void setHold(boolean hold) {
        this.hold = hold;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getOperationAmount() {
        return operationAmount;
    }

    public void setOperationAmount(double operationAmount) {
        this.operationAmount = operationAmount;
    }

    public int getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(int currencyCode) {
        this.currencyCode = currencyCode;
    }

    public double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public double getCashbackAmount() {
        return cashbackAmount;
    }

    public void setCashbackAmount(double cashbackAmount) {
        this.cashbackAmount = cashbackAmount;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getCounterEdrpou() {
        return counterEdrpou;
    }

    public void setCounterEdrpou(String counterEdrpou) {
        this.counterEdrpou = counterEdrpou;
    }

    public String getCounterIban() {
        return counterIban;
    }

    public void setCounterIban(String counterIban) {
        this.counterIban = counterIban;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", time=" + time +
                ", description='" + description + '\'' +
                ", mcc=" + mcc +
                ", originalMcc=" + originalMcc +
                ", hold=" + hold +
                ", amount=" + amount +
                ", operationAmount=" + operationAmount +
                ", currencyCode=" + currencyCode +
                ", commissionRate=" + commissionRate +
                ", cashbackAmount=" + cashbackAmount +
                ", balance=" + balance +
                ", comment='" + comment + '\'' +
                ", receiptId='" + receiptId + '\'' +
                ", invoiceId='" + invoiceId + '\'' +
                ", counterEdrpou='" + counterEdrpou + '\'' +
                ", counterIban='" + counterIban + '\'' +
                ", counterName='" + counterName + '\'' +
                '}';
    }
}
