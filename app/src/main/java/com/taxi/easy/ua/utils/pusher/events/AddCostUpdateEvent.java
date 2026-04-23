package com.taxi.easy.ua.utils.pusher.events;

public class AddCostUpdateEvent {
    private String addCost;

    public AddCostUpdateEvent(String addCost) {
        this.addCost = addCost;
    }

    public String getAddCost() {
        return addCost;
    }
}