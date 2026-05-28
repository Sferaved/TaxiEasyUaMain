package com.taxi.easy.ua.utils.model;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.weather.finish.PassengerNotifier;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;
import com.taxi.easy.ua.utils.pusher.events.CanceledStatusEvent;
import com.taxi.easy.ua.utils.pusher.events.OrderResponseEvent;

import org.greenrobot.eventbus.EventBus;

public class ExecutionStatusViewModel extends ViewModel {

    public static final String PREF_FINISH_ACTIVE_UID = "finish_active_uid";
    public static final String PREF_FINISH_DOUBLE_UID = "finish_double_uid";
    public static final String PREF_FINISH_DISPLAY_COST = "finish_display_cost_grivna";
    public static final String PREF_FINISH_CANCEL_IN_FLIGHT = "finish_cancel_in_flight";
    public static final String PREF_FINISH_USER_CANCELED = "finish_user_canceled";
    public static final String PREF_FINISH_CANCELED_UID = "finish_canceled_uid";

    private final MutableLiveData<String> canceledStatus = new MutableLiveData<>();
    private final MutableLiveData<OrderResponse> orderResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isTenMinutesRemaining = new MutableLiveData<>();
    public final LiveData<Boolean> isTenMinutesRemaining = _isTenMinutesRemaining;

    //
    private final MutableLiveData<String> transactionStatus = new MutableLiveData<>();
    public LiveData<String> getTransactionStatus() {
        Log.d("VIEWMODEL", "getTransactionStatus() called, returning: " + transactionStatus.getValue());
        return transactionStatus;
    }

    public void setTransactionStatus(String status) {
        Log.d("VIEWMODEL", "setTransactionStatus() called with: " + status);
        Log.d("VIEWMODEL", "Previous value: " + transactionStatus.getValue());
        Log.d("VIEWMODEL", "Call stack:", new Exception());
        transactionStatus.setValue(status);
    }



    //Проверка отмены по оплате

    public LiveData<String> getCanceledStatus() {return canceledStatus;}
    public void setCanceledStatus(String canceled) {
        Log.e("Pusher eventCanceled", "setCanceledStatus:" + canceled);
        if (Looper.getMainLooper().isCurrentThread()) {
            canceledStatus.setValue(canceled);
        } else {
            canceledStatus.postValue(canceled);
        }
        EventBus.getDefault().post(new CanceledStatusEvent(canceled));
    }
    // Добавление стоимости

    private final MutableLiveData<String> addCostViewUpdate = new MutableLiveData<>();
    public LiveData<String> getAddCostViewUpdate() {return addCostViewUpdate;}

    // ✅ ИЗМЕНЕННЫЙ МЕТОД
    public void setAddCostViewUpdate(String addCost) {
        Log.e("Pusher addCostViewUpdate", "addCostViewUpdate:" + addCost);
        if (Looper.getMainLooper().isCurrentThread()) {
            addCostViewUpdate.setValue(addCost);
        } else {
            addCostViewUpdate.postValue(addCost);
        }

        // UI финише — через LiveData observer (без EventBus, иначе +доплата дважды)
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
        orderResponse.postValue(response);
        Log.i("ViewModel", "Updating order response: " + response.getDispatchingOrderUid());
        EventBus.getDefault().post(new OrderResponseEvent(response));
    }
    public void clearOrderResponse() {
        orderResponse.setValue(null);
    }

    public void setIsTenMinutesRemaining(boolean value) {
        _isTenMinutesRemaining.setValue(value);
    }

    // Метод установки значения с учётом выполнения на главном потоке
    public void setIsTenMinutesRemainingFromBackground(boolean value) {
        _isTenMinutesRemaining.postValue(value);
    }

    // Метод чтения значения (getter)
    public Boolean getIsTenMinutesRemaining() {
        return _isTenMinutesRemaining.getValue();
    }

    // Метод чтения через LiveData для наблюдения
    public LiveData<Boolean> observeIsTenMinutesRemaining() {
        return isTenMinutesRemaining;
    }


    private final MutableLiveData<String> uidLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> paySystemStatusLiveData = new MutableLiveData<>();

    public ExecutionStatusViewModel() {
        // Initialize with current values
        uidLiveData.setValue(MainActivity.uid);
        paySystemStatusLiveData.setValue(MainActivity.paySystemStatus);
    }

    // Expose LiveData to observe uid changes
    public LiveData<String> getUid() {
        return uidLiveData;
    }

    // Expose LiveData to observe paySystemStatus changes
    public LiveData<String> getPaySystemStatus() {
        return paySystemStatusLiveData;
    }

    /**
     * Новый uid заказа (после доплаты / пересоздания): предыдущий сохраняем в uid_Double для отмены пары.
     */
    public void updateUid(String newUid) {
        if (newUid == null || newUid.isEmpty()) {
            return;
        }
        String current = uidLiveData.getValue();
        if (current == null || current.isEmpty()) {
            current = MainActivity.uid;
        }
        if (current != null && !current.isEmpty() && !current.equals(newUid)) {
            MainActivity.uid_Double = current;
            Log.d("VIEWMODEL", "updateUid: previous uid -> uid_Double=" + current + ", new=" + newUid);
        }
        MainActivity.uid = newUid;
        uidLiveData.setValue(newUid);
        persistFinishOrderSnapshot();
        PassengerNotifier.linkFinishOrderUidsAfterUidChange(current, newUid);
    }

    public void restoreUidFromPersisted(@Nullable String activeUid, @Nullable String doubleUid) {
        if (activeUid == null || activeUid.isEmpty()) {
            return;
        }
        MainActivity.uid = activeUid;
        MainActivity.uid_Double = doubleUid != null ? doubleUid : "";
        uidLiveData.setValue(activeUid);
    }

    public void persistDisplayCostGrivna(@Nullable String costGrivna) {
        if (costGrivna == null || costGrivna.isEmpty()) {
            return;
        }
        sharedPreferencesHelperMain.saveValue(PREF_FINISH_DISPLAY_COST, costGrivna);
    }

    public static void setCancelInFlightPref(boolean inFlight) {
        sharedPreferencesHelperMain.saveValue(PREF_FINISH_CANCEL_IN_FLIGHT, inFlight);
    }

    public static boolean isCancelInFlightPref() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_FINISH_CANCEL_IN_FLIGHT, false);
        return v instanceof Boolean && (Boolean) v;
    }

    public static void setUserCanceledPref(boolean canceled) {
        sharedPreferencesHelperMain.saveValue(PREF_FINISH_USER_CANCELED, canceled);
    }

    public static boolean isUserCanceledPref() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_FINISH_USER_CANCELED, false);
        return v instanceof Boolean && (Boolean) v;
    }

    @Nullable
    public static String getCanceledOrderUid() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_FINISH_CANCELED_UID, "");
        return v instanceof String && !((String) v).isEmpty() ? (String) v : null;
    }

    public static void resetNewOrderSession(@Nullable String activeOrderUid) {
        setCancelInFlightPref(false);
        setUserCanceledPref(false);
        sharedPreferencesHelperMain.saveValue(PREF_FINISH_CANCELED_UID, "");
        if (activeOrderUid != null && !activeOrderUid.isEmpty()) {
            MainActivity.uid = activeOrderUid;
            sharedPreferencesHelperMain.saveValue(PREF_FINISH_ACTIVE_UID, activeOrderUid);
        }
    }

    public static void markUserCanceledOrder(@Nullable String orderUid) {
        setUserCanceledPref(true);
        if (orderUid != null && !orderUid.isEmpty()) {
            sharedPreferencesHelperMain.saveValue(PREF_FINISH_CANCELED_UID, orderUid);
        }
    }

    public static boolean shouldBlockAddCost(@Nullable String orderUid) {
        if (isCancelInFlightPref()) {
            return true;
        }
        if (!isUserCanceledPref()) {
            return false;
        }
        String canceledUid = getCanceledOrderUid();
        if (canceledUid == null || canceledUid.isEmpty()) {
            return false;
        }
        return orderUid != null && orderUid.equals(canceledUid);
    }

    @Nullable
    public static String getPersistedActiveUid() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_FINISH_ACTIVE_UID, "");
        return v instanceof String && !((String) v).isEmpty() ? (String) v : null;
    }

    @Nullable
    public static String getPersistedDoubleUid() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_FINISH_DOUBLE_UID, "");
        return v instanceof String ? (String) v : null;
    }

    @Nullable
    public static String getPersistedDisplayCost() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_FINISH_DISPLAY_COST, "");
        return v instanceof String && !((String) v).isEmpty() ? (String) v : null;
    }

    private void persistFinishOrderSnapshot() {
        if (MainActivity.uid != null && !MainActivity.uid.isEmpty()) {
            sharedPreferencesHelperMain.saveValue(PREF_FINISH_ACTIVE_UID, MainActivity.uid);
        }
        if (MainActivity.uid_Double != null) {
            sharedPreferencesHelperMain.saveValue(PREF_FINISH_DOUBLE_UID, MainActivity.uid_Double);
        }
    }

    /** После успешной отмены на сервере — сброс uid в приложении (флаги отмены не трогаем). */
    public void clearOrderUid() {
        MainActivity.uid = null;
        MainActivity.uid_Double = null;
        uidLiveData.setValue(null);
        sharedPreferencesHelperMain.saveValue(PREF_FINISH_ACTIVE_UID, "");
        sharedPreferencesHelperMain.saveValue(PREF_FINISH_DOUBLE_UID, "");
        sharedPreferencesHelperMain.saveValue(PREF_FINISH_DISPLAY_COST, "");
        setCancelInFlightPref(false);
        PassengerNotifier.clearWeatherNoticePrefs();
    }

    // Method to update paySystemStatus
    public void updatePaySystemStatus(String newPaySystemStatus) {
        paySystemStatusLiveData.setValue(newPaySystemStatus);
    }

    private final MutableLiveData<Boolean> statusNalUpdate = new MutableLiveData<>();
    public LiveData<Boolean> getStatusNalUpdate() {return statusNalUpdate;}
    public void setStatusNalUpdate(Boolean canceled) {
        Log.e("startFinishPage", "StatusNalUpdate:" + canceled);
        if (Looper.getMainLooper().isCurrentThread()) {
            statusNalUpdate.setValue(canceled);
        } else {
            statusNalUpdate.postValue(canceled);
        }
    }

    private final MutableLiveData<Boolean> statusXUpdate = new MutableLiveData<>();
    public LiveData<Boolean> getStatusX() {return statusXUpdate;}
    public void setStatusX(Boolean statusX) {
        Log.e("setStatusX", "setStatusXUpdate:" + statusX);
        if (Looper.getMainLooper().isCurrentThread()) {
            statusXUpdate.setValue(statusX);
        } else {
            statusXUpdate.postValue(statusX);
        }
    }

    private final MutableLiveData<Boolean> statusGpsUpdate = new MutableLiveData<>();
    public LiveData<Boolean> getStatusGpsUpdate() {return statusGpsUpdate;}
    public void setStatusGpsUpdate(Boolean statusGps) {
        Log.e("setStatusGps", "setStatusGpsUpdate:" + statusGps);
        if (Looper.getMainLooper().isCurrentThread()) {
            statusGpsUpdate.setValue(statusGps);
        } else {
            statusGpsUpdate.postValue(statusGps);
        }
    }


    //Кнопак отмены
    private final MutableLiveData<Boolean> cancelButtonVisible = new MutableLiveData<>(true);

    public LiveData<Boolean> getCancelButtonVisible() {
        return cancelButtonVisible;
    }

    public void showCancelButton() {
        if (Looper.getMainLooper().isCurrentThread()) {
            cancelButtonVisible.setValue(true);
        } else {
            cancelButtonVisible.postValue(true);
        }
    }

    public void hideCancelButton() {
        if (Looper.getMainLooper().isCurrentThread()) {
            cancelButtonVisible.setValue(false);
        } else {
            cancelButtonVisible.postValue(false);
        }
    }

}