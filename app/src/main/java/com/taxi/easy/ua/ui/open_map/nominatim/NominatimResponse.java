package com.taxi.easy.ua.ui.open_map.nominatim;

import com.google.gson.annotations.SerializedName;

public class NominatimResponse {

    @SerializedName("place_id")
    private long placeId;

    @SerializedName("display_name")
    private String displayName;

    // Другие поля, если необходимо

    public long getPlaceId() {
        return placeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return "NominatimResponse{" +
                "placeId=" + placeId +
                ", displayName='" + displayName + '\'' +
                '}';
    }
// Другие геттеры, если необходимо
}
