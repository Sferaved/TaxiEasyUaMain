package com.taxi.easy.ua.ui.visicom.visicom_search.resp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeocodeGeoCentroid {

    @SerializedName("type")
    private String type;

    @SerializedName("coordinates")
    private List<Double> coordinates;

    @Override
    public String toString() {
        return "GeocodeGeoCentroid{" +
                "type='" + type + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }

    public String getType() {
        return type;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }
}
