package com.taxi.easy.ua.ui.fondy.status;

public interface StatusCallback {
    void onStatusReceived(String orderStatus);
    void onError(String errorMessage);
}
