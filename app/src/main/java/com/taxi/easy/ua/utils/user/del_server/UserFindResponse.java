package com.taxi.easy.ua.utils.user.del_server;

import com.google.gson.annotations.SerializedName;

public class UserFindResponse {
    @SerializedName("checkUser")
    private Boolean checkUser;

    public Boolean getCheckUser() {
        return checkUser;
    }
}
