package com.taxi.easy.ua.utils.pusher;

import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.viewModel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PusherManager {
    private static final String PUSHER_APP_KEY = "a10fb0bff91153d35f36"; // Ваш ключ
    private static final String PUSHER_CLUSTER = "mt1"; // Ваш кластер
    private static final String CHANNEL_NAME = "teal-towel-48"; // Канал

    private final String eventUid;
    private final String eventOrder;
    private final String eventTransactionStatus;
    private final String eventCanceled;
    private final String orderResponseEvent;
    private final String eventStartExecution;
    private static Pusher pusher = null;
    private boolean isSubscribed = false;
    public PusherManager(String eventSuffix, String userEmail) {
        this.eventUid = "order-status-updated-" + eventSuffix + "-" + userEmail;
        this.eventOrder = "order-" + eventSuffix + "-" + userEmail;
        this.eventTransactionStatus = "transactionStatus-" + eventSuffix + "-" + userEmail;
        this.eventCanceled = "eventCanceled-" + eventSuffix + "-" + userEmail;
        this.orderResponseEvent = "orderResponseEvent-" + eventSuffix + "-" + userEmail;
        this.eventStartExecution = "orderStartExecution-" + eventSuffix + "-" + userEmail;

        PusherOptions options = new PusherOptions();
        options.setCluster(PUSHER_CLUSTER);

        pusher = new Pusher(PUSHER_APP_KEY, options);
    }

    // Подключение к Pusher
    public void connect() {
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Log.i("Pusher", "State changed from " + change.getPreviousState() + " to " + change.getCurrentState());
                if (change.getCurrentState() == ConnectionState.CONNECTED) {
                    Log.i("Pusher", "Successfully connected to Pusher");
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Log.e("Pusher", "Error connecting: " + message + " (Code: " + code + ")", e);
            }
        }, ConnectionState.ALL);
    }

    // Подписка на канал и событие
    public void subscribeToChannel() {
        if (isSubscribed) return; // Предотвращает повторную подписку
        isSubscribed = true;

        Channel channel = pusher.subscribe(CHANNEL_NAME);
        Log.i("Pusher", "Subscribing to channel: " + CHANNEL_NAME);
        Log.i("Pusher", "Subscribing to event: " + eventUid);
        Log.i("Pusher", "Subscribing to eventOrder: " + eventOrder);
        Log.i("Pusher", "Subscribing to eventStatus: " + eventTransactionStatus);
        Log.i("Pusher", "Subscribing to eventCanceled: " + eventCanceled);
        Log.i("Pusher", "Subscribing to orderResponseEvent: " + orderResponseEvent);

        // Обработка события получения номера заказа после регистрации на сервере и определения нал/безнал для выбора способа опроса статусов
        channel.bind(eventUid, event -> {
            Log.i("Pusher", "Received event: " + event.toString());
            try {
                JSONObject eventData = new JSONObject(event.getData());
                String orderUid = eventData.getString("order_uid");
                String  paySystemStatus;
                if (eventData.has("paySystemStatus")) {
                    paySystemStatus = eventData.getString("paySystemStatus");
                } else {
                    paySystemStatus = "nal_payment";
                }
                Log.i("Pusher", "Order UID: " + orderUid);
                Log.i("Pusher", "paySystemStatus: " + paySystemStatus);
                // Переключаемся на главный поток для обновления UI и переменной
                new Handler(Looper.getMainLooper()).post(() -> {
                    MainActivity.uid = orderUid;

                    MainActivity.paySystemStatus = paySystemStatus;

                    if (FinishSeparateFragment.btn_cancel_order != null ) {
                        FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
                        FinishSeparateFragment.btn_cancel_order.setEnabled(true);
                        FinishSeparateFragment.btn_cancel_order.setClickable(true);
                    } else {
                        Log.e("Pusher", "btn_cancel_order is null!");
                    }
                });

            } catch (JSONException e) {
                Log.e("Pusher", "JSON Parsing error", e);
            }
        });

        //Получение статуса холда
        channel.bind(eventTransactionStatus, event -> {
            Log.i("Pusher", "Received event: " + event.toString());

            try {
                JSONObject eventData = new JSONObject(event.getData());
                String uid = eventData.getString("uid");
                String transactionStatus = eventData.getString("transactionStatus");
                Log.i("Pusher", "Parsed uid: " + uid);
                Log.i("Pusher", "Parsed Main uid: " + MainActivity.uid);
                Log.i("Pusher", "Parsed transactionStatus: " + transactionStatus);

                if(Objects.equals(MainActivity.uid, uid)) {
                    // Проверка на null перед переключением на главный поток
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Log.d("Pusher", "Updating UI with status: " + transactionStatus);

                        // Установка начального статуса транзакции
                        viewModel.setTransactionStatus(transactionStatus);
                        Log.i("Transaction", "Initial transaction status set: " + transactionStatus);

                        // Проверка UI элемента перед взаимодействием
                        if (FinishSeparateFragment.btn_cancel_order != null) {
                            FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
                            FinishSeparateFragment.btn_cancel_order.setEnabled(true);
                            FinishSeparateFragment.btn_cancel_order.setClickable(true);
                            Log.d("Pusher", "Cancel button enabled successfully");
                        } else {
                            Log.e("Pusher", "btn_cancel_order is null when updating status: " + transactionStatus);
                        }
                    });
                }


            } catch (JSONException e) {
                Log.e("Pusher", "JSON Parsing error for event: " + event.getData(), e);
            } catch (Exception e) {
                Log.e("Pusher", "Unexpected error processing Pusher event", e);
            }
        });


        //Получение статуса холда
        channel.bind(eventStartExecution, event -> {
            Log.i("Pusher", "Received event: " + event.toString());

            try {
                JSONObject eventData = new JSONObject(event.getData());
                String eventStartExecution = eventData.getString("eventStartExecution");
                Log.i("Pusher", "Parsed transactionStatus: " + eventStartExecution);

                // Проверка на null перед переключением на главный поток
                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.d("Pusher", "Updating UI with status: " + eventStartExecution);

                    // Проверка UI элемента перед взаимодействием
                    if (FinishSeparateFragment.btn_cancel_order != null) {
                        FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
                        FinishSeparateFragment.btn_cancel_order.setEnabled(true);
                        FinishSeparateFragment.btn_cancel_order.setClickable(true);
                        Log.d("Pusher", "Cancel button enabled successfully");
                    } else {
                        Log.e("Pusher", "btn_cancel_order is null when updating status: " + eventStartExecution);
                    }
                });

            } catch (JSONException e) {
                Log.e("Pusher", "JSON Parsing error for event: " + event.getData(), e);
            } catch (Exception e) {
                Log.e("Pusher", "Unexpected error processing Pusher event", e);
            }
        });

        // Получение статуса отмены заказа с сервера
        channel.bind(eventCanceled, event -> {
            Log.i("Pusher", "Received event: " + event.toString());

            try {
                JSONObject eventData = new JSONObject(event.getData());
                String canceled = eventData.getString("canceled");
                String uid = eventData.getString("uid");
                Log.i("Pusher", "Parsed eventCanceled: " + canceled);


                // Проверка, что uid существует и не null
                if (uid == null) {
                    Log.w("Pusher", "UID is null in orderResponse: " + eventData);
                } else if (MainActivity.uid != null && MainActivity.uid.equals(uid)) {
                    // Проверка на null перед переключением на главный поток
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Log.d("Pusher", "Updating UI with eventCanceled: " + canceled);

                        // Установка начального статуса транзакции
                        viewModel.setCanceledStatus(canceled);
                        Log.i("Transaction", "Initial canceled set: " + canceled);

                        // Проверка UI элемента перед взаимодействием
                        if (FinishSeparateFragment.btn_cancel_order != null) {
                            FinishSeparateFragment.btn_cancel_order.setVisibility(View.GONE);
                            FinishSeparateFragment.btn_again.setVisibility(VISIBLE);
                            Log.d("Pusher", "Cancel button enabled successfully");
                        } else {
                            Log.e("Pusher", "btn_cancel_order is null when updating  canceled status: " + canceled);
                        }
                    });
                }
                } catch(JSONException e){
                    Log.e("Pusher", "JSON Parsing error for event: " + event.getData(), e);
                } catch(Exception e){
                    Log.e("Pusher", "Unexpected error processing Pusher event", e);
                }

        });


        //Получение заказа из вилки с действием для отображения на фнинишной
        channel.bind(orderResponseEvent, event -> {
            Log.i("Pusher orderResponseEvent", "Received orderResponseEvent: " + event.toString());

            try {
                Gson gson = new Gson();

                // Сначала распарсим `event.getData()` как строку
                String jsonString = gson.fromJson(event.getData(), String.class);

                // Теперь распарсим результат в объект OrderResponse
                MainActivity.orderResponse = gson.fromJson(jsonString, OrderResponse.class);
                String uid = MainActivity.orderResponse.getUid();
                String action = MainActivity.orderResponse.getAction();

                Log.i("Pusher orderResponseEvent", "Received orderResponseEvent: " + MainActivity.orderResponse.getExecutionStatus());
                Log.i("Pusher uid", "Received uid: " + uid);
                Log.i("Pusher action", "Received action: " + action);

                // Проверка, что uid существует и не null
                if (uid == null) {
                    Log.w("Pusher", "UID is null in orderResponse: " + jsonString);
                } else if (MainActivity.uid != null && MainActivity.uid.equals(uid)) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Log.d("Pusher orderResponseEvent", "Updating UI with orderResponse");

                        viewModel.updateOrderResponse(MainActivity.orderResponse);
                        if (FinishSeparateFragment.btn_cancel_order != null) {
                            FinishSeparateFragment.btn_cancel_order.setVisibility(View.VISIBLE);
                            FinishSeparateFragment.btn_cancel_order.setEnabled(true);
                            FinishSeparateFragment.btn_cancel_order.setClickable(true);
                        } else {
                            Log.e("Pusher", "btn_cancel_order is null!");
                        }
                    });
                } else {
                    Log.d("Pusher", "UIDs do not match or MainActivity.uid is null. MainActivity.uid: " + MainActivity.uid + ", Response uid: " + uid);
                }

            } catch (JsonSyntaxException e) {
                Log.e("Pusher", "JSON Parsing error for event: " + event.getData(), e);
            } catch (Exception e) {
                Log.e("Pusher", "Unexpected error processing Pusher event", e);
            }

        });

        // Получение заказа с сервера после регистрации
        channel.bind(eventOrder, event -> {
            Log.i("Pusher", "Received eventOrder: " + event.toString());
            try {
                // Преобразуем данные события в JSONObject
                JSONObject eventData = new JSONObject(event.getData());

                // Создаём Map для хранения данных
                Map<String, String> eventValues = new HashMap<>();
                // Добавляем данные в Map
                eventValues.put("from_lat", eventData.optString("from_lat", "null"));
                eventValues.put("from_lng", eventData.optString("from_lng", "null"));
                eventValues.put("lat", eventData.optString("lat", "null"));
                eventValues.put("lng", eventData.optString("lng", "null"));
                eventValues.put("dispatching_order_uid", eventData.optString("dispatching_order_uid", "null"));
                eventValues.put("order_cost", eventData.optString("order_cost", "0"));
                eventValues.put("currency", eventData.optString("currency", "null"));
                eventValues.put("routefrom", eventData.optString("routefrom", "null"));
                eventValues.put("routefromnumber", eventData.optString("routefromnumber", "null"));
                eventValues.put("routeto", eventData.optString("routeto", "null"));
                eventValues.put("to_number", eventData.optString("to_number", "null"));
                eventValues.put("required_time", eventData.optString("required_time", ""));
                eventValues.put("flexible_tariff_name", eventData.optString("flexible_tariff_name", "null"));
                eventValues.put("comment_info", eventData.optString("comment_info", ""));
                eventValues.put("extra_charge_codes", eventData.optString("extra_charge_codes", ""));

                // Добавляем дополнительные поля, если они существуют

                String dispatchingOrderUidDouble = eventData.optString("dispatching_order_uid_Double", " ");
                eventValues.put("dispatching_order_uid_Double", dispatchingOrderUidDouble.equals(" ") ? " " : dispatchingOrderUidDouble);


                VisicomFragment.sendUrlMap = eventValues;
                // Логируем успешный случай
                    Log.i("Pusher", "Event Values: " + eventValues.toString());

            } catch (JSONException e) {
                // Логируем ошибку при парсинге JSON
                Log.e("Pusher", "JSON Parsing error", e);

                // Добавляем ошибку в Map
                Map<String, String> errorValues = new HashMap<>();
                errorValues.put("order_cost", "0");
                errorValues.put("message", "JSON Parsing error");
                Log.e("Pusher", "Error Values: " + errorValues.toString());
            }
        });

    }


    // Отключение
    public void disconnect() {
        if (pusher != null) {
            pusher.disconnect();
        }
    }

}
