package com.taxi.easy.ua.utils.location;

/**
 * Reverse geocode of a map pick must follow the place where the user stopped,
 * not an earlier pan or zoom whose reply arrived later.
 */
public final class MapPickGeocodeGate {

    private MapPickGeocodeGate() {
    }

    public static boolean shouldApplyGeocodeResult(int responseGeneration, int currentGeneration,
                                                    boolean gestureActive) {
        return responseGeneration > 0
                && responseGeneration == currentGeneration
                && !gestureActive;
    }

    /**
     * The user already placed the map. Recentering on a geocode reply pulls the
     * tiles back to a previous stop.
     */
    public static boolean shouldRecenterAfterGeocode(boolean userMovedMap) {
        return !userMovedMap;
    }
}
