package com.taxi.easy.ua.utils.clear;

import android.app.Activity;
import android.os.Bundle;

public class ClearTaskActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Немедленно завершаем эту активность после запуска
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Убеждаемся, что активность завершится даже при уходе в фон
        finish();
    }
}