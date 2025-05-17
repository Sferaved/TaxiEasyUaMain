package com.taxi.easy.ua.ui.finish.model;

import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.utils.pusher.events.CanceledStatusEvent;
import com.taxi.easy.ua.utils.pusher.events.OrderResponseEvent;

import org.greenrobot.eventbus.EventBus;

public class ExecutionStatusViewModel extends ViewModel {


    private final MutableLiveData<String> canceledStatus = new MutableLiveData<>();
    private final MutableLiveData<OrderResponse> orderResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isTenMinutesRemaining = new MutableLiveData<>();
    public final LiveData<Boolean> isTenMinutesRemaining = _isTenMinutesRemaining;

    //
    private final SingleLiveEvent<String> transactionStatus = new SingleLiveEvent<>();
    public LiveData<String> getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String status) {
        transactionStatus.postValue(status);
    }



    //Проверка отмены по оплате

    public LiveData<String> getCanceledStatus() {return canceledStatus;}
    public void setCanceledStatus(String canceled) {
        Log.e("Pusher eventCanceled", "setCanceledStatus:" + canceled);
        canceledStatus.postValue(canceled);
        EventBus.getDefault().post(new CanceledStatusEvent(canceled));

    }
    // Добавление стоимости

    private final MutableLiveData<String> addCostViewUpdate = new MutableLiveData<>();
    public LiveData<String> getAddCostViewUpdate() {return addCostViewUpdate;}
    public void setAddCostViewUpdate(String addCost) {
        Log.e("Pusher addCostViewUpdate", "addCostViewUpdate:" + addCost);
        if (Looper.getMainLooper().isCurrentThread()) {
            addCostViewUpdate.setValue(addCost);
        } else {
            addCostViewUpdate.postValue(addCost);
        }
    }

    private final MutableLiveData<Boolean> cancelStatus = new MutableLiveData<>();
    public LiveData<Boolean> getCancelStatus() {return cancelStatus;}
    public void setCancelStatus(Boolean canceled) {
        Log.e("Pusher canceled", "canceled:" + canceled);
        if (Looper.getMainLooper().isCurrentThread()) {
            cancelStatus.setValue(canceled);
        } else {
            cancelStatus.postValue(canceled);
        }
    }

    //Опрос вилки
    public LiveData<OrderResponse> getOrderResponse() {return orderResponse;}
    public void updateOrderResponse(OrderResponse response) {
        if (response == null) {
            Log.e("ViewModel", "Received null OrderResponse");
            return;
        }

        // Логируем полученные данные
        Log.i("ViewModel", "Updating order response: " + response.getDispatchingOrderUid());

        // Обновляем LiveData
        orderResponse.postValue(response); // Затем установка значения
        Log.i("ViewModel", "Updating order response: " + response.getDispatchingOrderUid());
        EventBus.getDefault().post(new OrderResponseEvent(response)); // Публикация события
    }
    public void clearOrderResponse() {
        orderResponse.setValue(null); // Сбрасываем в null
    }

    public void setIsTenMinutesRemaining(boolean value) {
        _isTenMinutesRemaining.setValue(value);
    }

    // Метод установки значения с учётом выполнения на главном потоке
    public void setIsTenMinutesRemainingFromBackground(boolean value) {
        _isTenMinutesRemaining.postValue(value); // Для вызовов из фонового потока
    }

    // Метод чтения значения (getter)
    public Boolean getIsTenMinutesRemaining() {
        return _isTenMinutesRemaining.getValue();
    }

    // Метод чтения через LiveData для наблюдения
    public LiveData<Boolean> observeIsTenMinutesRemaining() {
        return isTenMinutesRemaining;
    }


}
