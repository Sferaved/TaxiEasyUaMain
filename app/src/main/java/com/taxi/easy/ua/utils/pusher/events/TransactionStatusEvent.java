package com.taxi.easy.ua.utils.pusher.events;

public class TransactionStatusEvent {
    private final String status;

    public TransactionStatusEvent(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}