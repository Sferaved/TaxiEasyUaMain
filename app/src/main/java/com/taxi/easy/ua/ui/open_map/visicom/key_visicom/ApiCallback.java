package com.taxi.easy.ua.ui.open_map.visicom.key_visicom;

public interface ApiCallback {
    void onVisicomKeyReceived(String key);
    void onApiError(int errorCode);
    void onApiFailure(Throwable t);
}

