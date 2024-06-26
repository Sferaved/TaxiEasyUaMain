package com.taxi.easy.ua.utils.ip.ip_util_retrofit;

import retrofit2.Call;
import retrofit2.http.GET;

public interface IpifyService {
    @GET("/?format=json")
    Call<IpResponse> getPublicIPAddress();
}
