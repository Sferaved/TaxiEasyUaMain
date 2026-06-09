package com.taxi.easy.ua.utils.ui;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class ScreenInsetsHelper {

    private ScreenInsetsHelper() {
    }

    public static void applyScrollableScreenPadding(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            float density = v.getResources().getDisplayMetrics().density;
            int horizontal = (int) (16 * density);
            int bottomExtra = (int) (16 * density);
            v.setPadding(horizontal, systemBars.top, horizontal, systemBars.bottom + bottomExtra);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
