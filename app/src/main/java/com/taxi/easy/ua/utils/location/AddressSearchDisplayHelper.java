package com.taxi.easy.ua.utils.location;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Форматирование подсказок Visicom: маркеры \f (улица) и \t (полный адрес),
 * проверка наличия номера дома в тексте результата.
 */
public final class AddressSearchDisplayHelper {

    public static final char STREET_MARKER = '\f';
    public static final char COMPLETE_MARKER = '\t';

    private AddressSearchDisplayHelper() {
    }

    @Nullable
    public static String toDisplayLabel(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(String.valueOf(STREET_MARKER), "")
                .replace(String.valueOf(COMPLETE_MARKER), "")
                .trim();
    }

    public static boolean hasHouseNumberDigit(@Nullable String raw) {
        String label = toDisplayLabel(raw);
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c >= '0' && c <= '9') {
                return true;
            }
        }
        return false;
    }

    public static boolean anyResultHasHouseNumber(@Nullable List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }
        for (String address : addresses) {
            if (hasHouseNumberDigit(address)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStreetOnly(@Nullable String raw) {
        return raw != null && raw.indexOf(STREET_MARKER) >= 0;
    }

    public static boolean isComplete(@Nullable String raw) {
        return raw != null && raw.indexOf(COMPLETE_MARKER) >= 0;
    }

    /** Ни один результат не содержит цифр — можно применить адрес без номера дома. */
    public static boolean shouldOfferApplyWithoutHouse(@Nullable List<String> addresses) {
        return addresses != null && !addresses.isEmpty()
                && !anyResultHasHouseNumber(addresses);
    }
}
