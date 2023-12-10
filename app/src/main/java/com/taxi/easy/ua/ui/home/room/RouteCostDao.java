package com.taxi.easy.ua.ui.home.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RouteCostDao {
    @Insert
    void insert(RouteCost routeCost);

    @Update
    void update(RouteCost routeCost); // Добавляем метод обновления записи

    @Query("SELECT * FROM routecost WHERE routeId = :routeId")
    RouteCost getRouteCost(int routeId);
}


