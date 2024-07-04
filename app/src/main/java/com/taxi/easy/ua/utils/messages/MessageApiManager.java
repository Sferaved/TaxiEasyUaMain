package com.taxi.easy.ua.utils.messages;

import java.util.List;

import retrofit2.Call;

public class MessageApiManager {

    private final MessageApiService messageApiService;

    public MessageApiManager() {
        messageApiService = ApiClientMessage.getClient().create(MessageApiService.class);
    }

    public Call<List<Message>> getMessages(String email, String app_name) {
        return messageApiService.getMessages(email, app_name);
    }
}
