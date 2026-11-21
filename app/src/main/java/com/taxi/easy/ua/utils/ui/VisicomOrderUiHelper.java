package com.taxi.easy.ua.utils.ui;

import androidx.annotation.Nullable;

/**
 * Правила видимости экрана заказа (Visicom).
 * Mantis #121: поля адресов не должны оставаться invisible после onResume / retry.
 */
public final class VisicomOrderUiHelper {

    private VisicomOrderUiHelper() {
    }

    /**
     * Кнопки заказа (цена, заказать, допы) — только когда город готов и есть сеть.
     * Поля «Звідки/Куди» показываются отдельно и не зависят от этого флага.
     */
    public static boolean shouldShowOrderActionButtons(
            @Nullable String visibleShed,
            boolean networkAvailable
    ) {
        if (!networkAvailable) {
            return false;
        }
        if (visibleShed == null || visibleShed.isEmpty()) {
            return false;
        }
        return !"no".equalsIgnoreCase(visibleShed.trim());
    }
}

