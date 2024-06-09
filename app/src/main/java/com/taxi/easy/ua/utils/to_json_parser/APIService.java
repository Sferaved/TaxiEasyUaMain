package com.taxi.easy.ua.utils.to_json_parser;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface APIService {

    @GET
    Call<JsonResponse> getData(@Url String urlString);
}