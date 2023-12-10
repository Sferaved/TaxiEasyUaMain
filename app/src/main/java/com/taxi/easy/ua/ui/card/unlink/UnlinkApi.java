package com.taxi.easy.ua.ui.card.unlink;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface UnlinkApi {
    @GET("/delete-card-token/{rectoken}")
    Call<Void> deleteCardTokenFondy(
            @Path("rectoken") String rectoken
    );

}

