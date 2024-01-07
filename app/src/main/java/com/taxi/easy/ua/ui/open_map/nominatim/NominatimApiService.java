package com.taxi.easy.ua.ui.open_map.nominatim;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NominatimApiService {

    @GET("/search")
    Call<List<NominatimPlace>> search(
            @Query("q") String query,
            @Query("format") String format
//            @Query("addressdetails") int addressDetails,
//            @Query("limit") int limit,
//            @Query("street") String street,  // Добавлен параметр для улицы
//            @Query("city") String city,      // Добавлен параметр для города
//            @Query("country") String country

    );

    // Другие методы, если необходимо
}

