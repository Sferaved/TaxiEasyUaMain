package com.taxi.easy.ua.utils.retrofit;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface APIService {
    @GET
    Call<Map<String, String>> getData(@Url String urlString);

}
