package com.taxi.easy.ua.utils.messages;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface MessageApiService {

    @GET("showMessage/{email}/{app_name}")
    Call<List<Message>> getMessages(
            @Path("email") String email,
            @Path("app_name") String app_name
            );
}

