package com.taxi.easy.ua.ui.finish;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface ApiService {
    @GET
    Call<Status> cancelOrder(@Url String url);

    @GET
    Call<OrderResponse> statusOrder(@Url String url);


    @GET("/ip/city")
    Call<City> cityOrder();

    @GET()
    Call<List<RouteResponse>> getRoutes(@Url String url);
    @GET()
    Call<BonusResponse> getBonus(@Url String url);

    @POST("/android/Universal/startNewProcessExecutionStatusPost/")
    Call<Void> startNewProcessExecutionStatus(@Body ProcessExecutionStatusRequest request);
    @GET("/android/Universal/startNewProcessExecutionStatus/{doubleOrder}")
    Call<Void> startNewProcessExecutionStatus(
            @Path("doubleOrder") String doubleOrder
    );
    @GET("/android/orderIdMemory/{order_id}/{uid}/{paySystem}")
    Call<Void> orderIdMemory(
            @Path("order_id") String fondy_order_id,
            @Path("uid") String uid,
            @Path("paySystem") String paySystem
    );

}

