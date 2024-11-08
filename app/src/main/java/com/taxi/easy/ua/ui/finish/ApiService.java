package com.taxi.easy.ua.ui.finish;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface ApiService {
    @GET
    Call<Status> cancelOrder(@Url String url);
    @GET
    Call<Status> cancelOrderDouble(@Url String url);

    @GET
    Call<OrderResponse> statusOrder(@Url String url);
    @GET
    Call<Void> drivercarposition(@Url String url);

    @GET
    Call<Void> calculateTimeToStart(@Url String url);

    @GET()
    Call<List<RouteResponse>> getRoutes(@Url String url);
    @GET()
    Call<List<RouteResponseCancel>> getRoutesCancel(@Url String url);
    @GET()
    Call<BonusResponse> getBonus(@Url String url);


    @GET("/android/orderIdMemory/{order_id}/{uid}/{paySystem}")
    Call<Void> orderIdMemory(
            @Path("order_id") String fondy_order_id,
            @Path("uid") String uid,
            @Path("paySystem") String paySystem
    );

    @GET("/android/startAddCostUpdate/{uid}/{typeAdd}")
    Call<Void> startAddCostUpdate(
            @Path("uid") String uid,
            @Path("typeAdd") String typeAdd
    );

    @GET("apiTest/android/searchOrderToDelete/{originLatitude}/{originLongitude}/{toLatitude}/{toLongitude}/{email}/{start}/{finish}/{payment_type}/{city}/{application}/")
    Call<Void> searchOrderToDelete(
            @Path("originLatitude") String originLatitude,
            @Path("originLongitude") String originLongitude,
            @Path("toLatitude") String toLatitude,
            @Path("toLongitude") String toLongitude,
            @Path("email") String email,
            @Path("start") String start,
            @Path("finish") String finish,
            @Path("payment_type") String payment_type,
            @Path("city") String city,
            @Path("application") String application
    );
}

