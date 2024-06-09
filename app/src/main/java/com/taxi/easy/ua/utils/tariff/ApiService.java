package com.taxi.easy.ua.utils.tariff;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("costSearchMarkersLocalTariffs/{lat1}/{lon1}/{lat2}/{lon2}/{user}/{services}/{city}/{application}")
    Call<List<Tariff>> getOrderCostDetails(
            @Path("lat1") Double lat1,
            @Path("lon1") Double lon1,
            @Path("lat2") Double lat2,
            @Path("lon2") Double lon2,
            @Path("user") String user,
            @Path("services") String services,
            @Path("city") String city,
            @Path("application") String application);
}
