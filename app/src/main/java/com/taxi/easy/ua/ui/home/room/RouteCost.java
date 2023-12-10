package com.taxi.easy.ua.ui.home.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity
public class RouteCost {
    @PrimaryKey
    public int routeId;
    public String from;
    public String fromNumber;
    public String to;
    public String toNumber;
    public String text_view_cost;
    public String addCost;
    public String tarif;
    public String payment_type;
    public List<String> servicesChecked;
    // Другие поля, связанные с стоимостью маршрута
}

