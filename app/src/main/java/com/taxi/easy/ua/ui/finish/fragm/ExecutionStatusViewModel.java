package com.taxi.easy.ua.ui.finish.fragm;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.taxi.easy.ua.ui.finish.OrderResponse;

public class ExecutionStatusViewModel extends ViewModel {

    private final MutableLiveData<String> transactionStatus = new MutableLiveData<>();
    private final MutableLiveData<String> canceledStatus = new MutableLiveData<>();
    private final MutableLiveData<OrderResponse> orderResponse = new MutableLiveData<>();


    //
    public LiveData<String> getTransactionStatus() {
        return transactionStatus;
    }
    public void setTransactionStatus(String TransactionStatus) {transactionStatus.postValue(TransactionStatus); }


    //Проверка отмены по оплате
    public LiveData<String> getCanceledStatus() {return canceledStatus;}
    public void setCanceledStatus(String canceled) {canceledStatus.postValue(canceled);}

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
        orderResponse.postValue(response);
    }
    public void clearOrderResponse() {
        orderResponse.setValue(null); // Сбрасываем в null
    }

}
