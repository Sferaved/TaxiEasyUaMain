package com.taxi.easy.ua.ui.cities.api;

import com.google.gson.annotations.SerializedName;

public class CityResponseMerchantFondy {
    @SerializedName("merchant_fondy")
    private String merchantFondy;

    @SerializedName("fondy_key_storage")
    private String fondyKeyStorage;

    public String getMerchantFondy() {
        return merchantFondy;
    }

    public String getFondyKeyStorage() {
        return fondyKeyStorage;
    }
}
