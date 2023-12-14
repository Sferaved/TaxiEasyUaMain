package com.taxi.easy.ua.ui.open_map.visicom.key;

public interface ApiCallback {
    void onVisicomKeyReceived(String key);
    void onApiError(int errorCode);
    void onApiFailure(Throwable t);
}

