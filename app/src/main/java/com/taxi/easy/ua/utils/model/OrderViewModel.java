package com.taxi.easy.ua.utils.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class OrderViewModel extends ViewModel {
    private final MutableLiveData<String> orderCostLiveData = new MutableLiveData<>();

    public LiveData<String> getOrderCost() {
        return orderCostLiveData;
    }

    public void setOrderCost(String cost) {
        orderCostLiveData.postValue(cost);
    }
}
