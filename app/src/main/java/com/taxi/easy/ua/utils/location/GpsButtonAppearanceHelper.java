package com.taxi.easy.ua.utils.location;

/**
 * Цвет кнопки GPS: зелёный только при включённом GPS и выданном ACCESS_FINE_LOCATION.
 * Крестик (ручной адрес) не перекрывает жёлтый/красный при отсутствии разрешения.
 */
public final class GpsButtonAppearanceHelper {

    public enum Appearance {
        GREEN_CROSS,
        GREEN,
        YELLOW,
        RED
    }

    private GpsButtonAppearanceHelper() {
    }

    public static Appearance resolve(boolean showCross, boolean gpsEnabled, boolean hasFineLocationPermission) {
        if (!gpsEnabled) {
            return Appearance.RED;
        }
        if (!hasFineLocationPermission) {
            return Appearance.YELLOW;
        }
        return showCross ? Appearance.GREEN_CROSS : Appearance.GREEN;
    }
}
