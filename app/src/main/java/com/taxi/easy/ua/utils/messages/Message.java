package com.taxi.easy.ua.utils.messages;

import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("text_message")
    private String textMessage;

    @SerializedName("sent_message_info")
    private int sentMessageInfo;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public String getTextMessage() {
        return textMessage;
    }
// Getters and setters...
}
