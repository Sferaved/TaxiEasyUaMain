package com.taxi.easy.ua.ui.open_map.mapbox;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MapboxResponse {
    @SerializedName("features")
    private List<Feature> features;

    public List<Feature> getFeatures() {
        return features;
    }
}
