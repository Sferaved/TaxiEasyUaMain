package com.taxi.easy.ua.utils.tariff;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("costSearchMarkersLocalTariffsTime/{originLatitude}/{originLongitude}/{toLatitude}/{toLongitude}/{user}/{time}/{date}/{services}/{city}/{application}")
    Call<List<Tariff>> getOrderCostDetails(
            @Path("originLatitude") Double lat1,
            @Path("originLongitude") Double lon1,
            @Path("toLatitude") Double lat2,
            @Path("toLongitude") Double lon2,
            @Path("user") String user,
            @Path("time") String time,
            @Path("date") String date,
            @Path("services") String services,
            @Path("city") String city,
            @Path("application") String application);
}
