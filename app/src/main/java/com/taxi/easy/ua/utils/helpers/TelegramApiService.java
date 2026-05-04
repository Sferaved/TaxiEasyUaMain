package com.taxi.easy.ua.utils.helpers;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface TelegramApiService {

    // Метод для отправки документа с caption
    @Multipart
    @POST("sendDocument")
    Call<Void> sendDocument(
            @Part("chat_id") RequestBody chatId,
            @Part("caption") RequestBody caption,
            @Part MultipartBody.Part document
    );

    // Метод для отправки документа без caption
    @Multipart
    @POST("sendDocument")
    Call<Void> sendDocumentWithoutCaption(
            @Part("chat_id") RequestBody chatId,
            @Part MultipartBody.Part document
    );

    // Метод для отправки текстового сообщения
    @FormUrlEncoded
    @POST("sendMessage")
    Call<Void> sendMessage(
            @Field("chat_id") String chatId,
            @Field("text") String text
    );
}

