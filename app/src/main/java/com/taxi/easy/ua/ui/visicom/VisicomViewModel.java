package com.taxi.easy.ua.ui.visicom;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

public class VisicomViewModel extends ViewModel {

    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Boolean> shouldReloadCost = new MutableLiveData<>(false);

    public VisicomViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
    }

    public void onAppForegrounded() {
        shouldReloadCost.setValue(true);
    }

    public LiveData<Boolean> getShouldReloadCost() {
        return shouldReloadCost;
    }

    public void costReloaded() {
        shouldReloadCost.setValue(false);
    }
}


