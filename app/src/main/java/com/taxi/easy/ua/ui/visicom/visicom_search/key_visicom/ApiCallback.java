package com.taxi.easy.ua.ui.visicom.visicom_search.key_visicom;

public interface ApiCallback {
    void onVisicomKeyReceived(String key);
    void onApiError(int errorCode);
    void onApiFailure(Throwable t);
}

