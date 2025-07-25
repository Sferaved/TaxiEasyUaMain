package com.taxi.easy.ua.ui.finish;

import com.taxi.easy.ua.service.AutoOrderResponse;
import com.taxi.easy.ua.service.OrderServiceResponse;

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
    Call<OrderServiceResponse> searchAutoOrderService(@Url String url);
    @GET
    Call<AutoOrderResponse> searchAutoOrderServiceAll(@Url String url);
    @GET
    Call<OrderResponse> getOrderStatusMessageResultPush(@Url String url);
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


    @GET("android/orderIdMemory/{order_id}/{uid}/{paySystem}")
    Call<Void> orderIdMemory(
            @Path("order_id") String fondy_order_id,
            @Path("uid") String uid,
            @Path("paySystem") String paySystem
    );
    @GET("android/wfpInvoice/{order_id}/{amount}/{uid}")
    Call<Void> wfpInvoice(
            @Path("order_id") String fondy_order_id,
            @Path("amount") String amount,
            @Path("uid") String uid
    );

    @GET("android/startAddCostUpdate/{uid}/{typeAdd}")
    Call<Status> startAddCostUpdate(
            @Path("uid") String uid,
            @Path("typeAdd") String typeAdd
    );

    @GET("android/startAddCostWithAddBottomUpdate/{uid}/{cost}")
    Call<Status> startAddCostWithAddBottomUpdate(
            @Path("uid") String uid,
            @Path("cost") String cost
    );

    @GET("android/startAddCostCardBottomUpdate/{uid}/{uid_Double}/{pay_method}/{order_id}/{city}/{addCost}")
    Call<Status> startAddCostCardBottomUpdate(
            @Path("uid") String uid,
            @Path("uid_Double") String uid_Double,
            @Path("pay_method") String pay_method,
            @Path("order_id") String order_id,
            @Path("city") String city,
            @Path("addCost") String addCost
    );

    @GET("api/check")
    Call<Status> check();
}

