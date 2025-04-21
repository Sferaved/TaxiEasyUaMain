package com.taxi.easy.ua.utils.pusher.events;

public class CanceledStatusEvent {
    private final String canceledStatus;

    public CanceledStatusEvent(String canceledStatus) {
        this.canceledStatus = canceledStatus;
    }

    public String getCanceledStatus() {
        return canceledStatus;
    }
}
