package com.taxi.easy.ua.ui.payment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface MonoBankApiService {
    @GET("personal/statement/{account}/{from}/{to}")
    Call<List<Transaction>> getTransactions(
            @Header("X-Token") String token,
            @Path("account") String account,
            @Path("from") String from,
            @Path("to") String to
    );
    @GET("personal/client-info")
    Call<ClientInfo> getClientInfo(@Header("X-Token") String token);

}



