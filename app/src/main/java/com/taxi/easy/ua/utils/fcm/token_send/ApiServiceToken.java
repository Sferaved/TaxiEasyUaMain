package com.taxi.easy.ua.utils.fcm.token_send;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiServiceToken {
    @GET("android_token_local/store/{email}/{app}/{token}/{local}/")
    Call<Void> sendToken(
            @Path("email") String email,
            @Path("app") String app,
            @Path("token") String token,
            @Path("local") String local
    );

    @GET("android_installation/register/{installationId}/{app}/{token}/{local}/{tz}/")
    Call<Void> registerInstallation(
            @Path("installationId") String installationId,
            @Path("app") String app,
            @Path("token") String token,
            @Path("local") String local,
            @Path("tz") String tz
    );

    @GET("android_installation/schedule_login_reminder/{installationId}/{app}/{local}/{tz}/")
    Call<Void> scheduleLoginReminder(
            @Path("installationId") String installationId,
            @Path("app") String app,
            @Path("local") String local,
            @Path("tz") String tz
    );

    @GET("android_installation/cancel_login_reminder/{installationId}/{app}/")
    Call<Void> cancelLoginReminder(
            @Path("installationId") String installationId,
            @Path("app") String app
    );
}