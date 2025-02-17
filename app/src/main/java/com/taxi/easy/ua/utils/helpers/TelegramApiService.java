package com.taxi.easy.ua.utils.helpers;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface TelegramApiService {
    @GET("sendMessage")
    Call<Void> sendMessage(
            @Query("chat_id") String chatId,
            @Query("text") String text
    );


    @Multipart
    @POST("sendDocument")
    Call<Void> sendDocument(@Query("chat_id") String chatId,
                            @Part("caption") RequestBody caption,
                            @Part MultipartBody.Part file);


}

