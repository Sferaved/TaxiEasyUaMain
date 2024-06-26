package com.taxi.easy.ua.utils.ip.ip_util_retrofit;

import com.google.gson.annotations.SerializedName;

public class IpResponse {
    @SerializedName("ip")
    private String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
