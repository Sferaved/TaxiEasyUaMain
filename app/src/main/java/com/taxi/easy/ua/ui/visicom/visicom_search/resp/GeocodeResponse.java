package com.taxi.easy.ua.ui.visicom.visicom_search.resp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeocodeResponse {

    @SerializedName("type")
    private String type;

    @SerializedName("id")
    private String id;

    @SerializedName("properties")
    private GeocodeProperties properties;

    @SerializedName("bbox")
    private List<Double> bbox;

    @SerializedName("geo_centroid")
    private GeocodeGeoCentroid geoCentroid;

    @SerializedName("url")
    private String url;

    public String getType() {
        return type;
    }

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

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "GeocodeResponse{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", properties=" + properties +
                ", bbox=" + bbox +
                ", geoCentroid=" + geoCentroid +
                ", url='" + url + '\'' +
                '}';
    }
}
