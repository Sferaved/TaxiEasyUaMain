package com.taxi.easy.ua.utils.location;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Форматирование подсказок Visicom: маркеры \f (улица) и \t (полный адрес),
 * проверка наличия номера дома в тексте результата.
 */
public final class AddressSearchDisplayHelper {

    public static final char STREET_MARKER = '\f';
    public static final char COMPLETE_MARKER = '\t';

    /** Площі, сквери, парки — без обов'язкового номера будинку (на відміну від вул./пр. тощо). */
    private static final String[] PLACE_PREFIXES_WITHOUT_HOUSE = {
            "пл.", "пл ", "скв.", "сквер", "майдан", "парк ", "парк.", "наб.", "спуск "
    };

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

    /**
     * Площа/сквер/парк с маркером \f — можна застосувати без номера будинку.
     * Звичайна вулиця (вул., пр., …) з \f — номер обов'язковий.
     */
    public static boolean isPlaceWithoutRequiredHouse(@Nullable String raw) {
        if (raw == null || isComplete(raw) || !isStreetOnly(raw)) {
            return false;
        }
        String label = toDisplayLabel(raw).toLowerCase(Locale.ROOT).trim();
        for (String prefix : PLACE_PREFIXES_WITHOUT_HOUSE) {
            if (label.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Повна адреса (\t) або площа/POI без обов'язкового номера будинку. */
    public static boolean canApplyWithoutHouseNumber(@Nullable String raw) {
        if (raw == null) {
            return false;
        }
        return isComplete(raw) || isPlaceWithoutRequiredHouse(raw);
    }

    /**
     * Показати «застосувати»: у списку є площа/повна адреса без обов'язкового номера будинку.
     * Звичайні вулиці з \f не враховуються — після вибору вулиці потрібен номер будинку.
     */
    public static boolean shouldOfferApplyWithoutHouse(@Nullable List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }
        for (String address : addresses) {
            if (canApplyWithoutHouseNumber(address)) {
                return true;
            }
        }
        return false;
    }
}
