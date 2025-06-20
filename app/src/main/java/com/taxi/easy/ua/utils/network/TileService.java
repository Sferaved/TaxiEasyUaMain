package com.taxi.easy.ua.utils.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TileService {
    @GET("{z}/{x}/{y}.png")
    Call<ResponseBody> downloadTile(
            @Path("z") int zoom,
            @Path("x") long x,
            @Path("y") long y
    );
}