package com.taxi.easy.ua.ui.cities.check;

import androidx.annotation.Nullable;

/**
 * Бегущий индикатор на нажатой кнопке города.
 * Показывается локально, без ожидания ответа сервера.
 */
public final class CityPressIndicatorPolicy {

    public static final long DURATION_MS = 60_000L;

    private CityPressIndicatorPolicy() {
    }

    public static boolean shouldShow(@Nullable String city) {
        return city != null && !city.trim().isEmpty();
    }
}
