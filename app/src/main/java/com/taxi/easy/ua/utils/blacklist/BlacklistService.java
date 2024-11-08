package com.taxi.easy.ua.utils.blacklist;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BlacklistService {
    @GET("blacklist/addAndroidToBlacklist/{email}")
    Call<Void> addToBlacklist(@Path("email") String email);
}
