package com.taxi.easy.ua.ui.open_map.visicom.resp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeocodeFeature {

    @SerializedName("id")
    private String id;

    @SerializedName("properties")
    private GeocodeProperties properties;

    @SerializedName("bbox")
    private List<Double> bbox;

    @SerializedName("geo_centroid")
    private GeocodeGeoCentroid geoCentroid;

    public String getId() {
        return id;
    }

    public GeocodeProperties getProperties() {
        return properties;
    }

    public List<Double> getBbox() {
        return bbox;
    }

    public GeocodeGeoCentroid getGeoCentroid() {
        return geoCentroid;
    }
}
