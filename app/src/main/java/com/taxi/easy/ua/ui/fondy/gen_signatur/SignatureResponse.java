package com.taxi.easy.ua.ui.fondy.gen_signatur;

import com.google.gson.annotations.SerializedName;

public class SignatureResponse {
    @SerializedName("digest")
    private String digest;

    public String getDigest() {
        return digest;
    }
}

