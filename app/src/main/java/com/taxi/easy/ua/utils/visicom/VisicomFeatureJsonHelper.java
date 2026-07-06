package com.taxi.easy.ua.utils.visicom;

import androidx.annotation.Nullable;

import org.json.JSONObject;

/** Guards for Visicom GeoJSON — single Feature vs FeatureCollection. */
public final class VisicomFeatureJsonHelper {

    private VisicomFeatureJsonHelper() {
    }

    public static boolean isFeatureCollection(@Nullable JSONObject json) {
        return json != null && json.has("features");
    }

    public static boolean hasRootFeatureFields(@Nullable JSONObject json) {
        return json != null && json.has("properties") && json.has("geo_centroid");
    }

    public static boolean hasFeatureItemFields(@Nullable JSONObject feature) {
        return feature != null && feature.has("properties") && feature.has("geo_centroid");
    }
}
