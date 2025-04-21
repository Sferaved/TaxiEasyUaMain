package com.taxi.easy.ua.utils.pusher;

import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.viewModel;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PusherManager {
    private static final String PUSHER_APP_KEY = "a10fb0bff91153d35f36"; // Ваш ключ
    private static final String PUSHER_CLUSTER = "mt1"; // Ваш кластер
    private static final String CHANNEL_NAME = "teal-towel-48"; // Канал

    private final String eventUid;
    private final String eventOrder;
    private final String eventTransactionStatus;
    private final String eventCanceled;
//    private final String orderResponseEvent;
//    private final String eventStartExecution;
    private static Pusher pusher = null;
    private boolean isSubscribed = false;
    Channel channel;
   Activity context;

    private final Set<String> boundEvents = new HashSet<>();
    public PusherManager(String eventSuffix, String userEmail, Activity context) {
        this.eventUid = "order-status-updated-" + eventSuffix + "-" + userEmail;
        this.eventOrder = "order-" + eventSuffix + "-" + userEmail;
        this.eventTransactionStatus = "transactionStatus-" + eventSuffix + "-" + userEmail;
        this.eventCanceled = "eventCanceled-" + eventSuffix + "-" + userEmail;
//        this.orderResponseEvent = "orderResponseEvent-" + eventSuffix + "-" + userEmail;
        this.context = context;
//        this.eventStartExecution = "orderStartExecution-" + eventSuffix + "-" + userEmail;

        PusherOptions options = new PusherOptions();
        options.setCluster(PUSHER_CLUSTER);

        pusher = new Pusher(PUSHER_APP_KEY, options);
    }

    // Подключение к Pusher с улучшенным логированием и обработкой ошибок
    private final boolean isShuttingDown = false; // Флаг завершения работы приложения

    public void connect() {
        if (pusher == null) {
            Logger.d(context,"Pusher", "Pusher instance is not initialized!");
            return;
        }

        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Logger.d(context,"Pusher", "State changed from " + change.getPreviousState() + " to " + change.getCurrentState());

                // Обработка успешного подключения
                if (change.getCurrentState() == ConnectionState.CONNECTED) {
                    Logger.d(context,"Pusher", "Successfully connected to Pusher");
                }

                // Логика повторного подключения
                if (change.getCurrentState() == ConnectionState.DISCONNECTED) {
                    Log.w("Pusher", "Disconnected from Pusher. Attempting reconnection...");

                    // Предотвращаем попытки подключения во время завершения приложения
                    if (!isShuttingDown) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                Logger.d(context,"Pusher", "Attempting reconnection...");
                                pusher.connect(new ConnectionEventListener() {
                                    @Override
                                    public void onConnectionStateChange(ConnectionStateChange innerChange) {
                                        Logger.d(context,"Pusher", "Reconnect: State changed from " +
                                                innerChange.getPreviousState() + " to " +
                                                innerChange.getCurrentState());
                                    }

                                    @Override
                                    public void onError(String message, String code, Exception e) {
                                        Logger.e(context,"Pusher", "Reconnect error: " + message + " (Code: " + code + ")" + e);
                                    }
                                }, ConnectionState.ALL);

                            } catch (Exception e) {
                                Logger.e(context,"Pusher", "Reconnect attempt failed: " + e);
                            }
                        }, 5000); // Задержка перед попыткой повторного подключения
                    } else {
                        Log.w("Pusher", "Reconnect skipped: Application is shutting down");
                    }
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Logger.e(context,"Pusher", "Error connecting: " + message + " (Code: " + code + ")" +  e);
                if (e != null) {
                    e.printStackTrace();
                }
            }
        }, ConnectionState.ALL);
    }





    // Приватный метод для проверки, привязано ли событие
    private boolean isEventBound(String eventName) {
        return boundEvents.contains(eventName);
    }

    // Метод для биндинга события с проверкой на дублирование
    public void bindEvent(String eventName, SubscriptionEventListener listener) {
        if (!isEventBound(eventName)) {
            channel.bind(eventName, listener);
            boundEvents.add(eventName);
        }
    }


    // Подписка на канал и событие


    public void subscribeToChannel() {
        if (isSubscribed) return; // Предотвращает повторную подписку
        isSubscribed = true;


        channel = pusher.subscribe(CHANNEL_NAME);
        Logger.d(context,"Pusher", "Subscribing to channel: " + CHANNEL_NAME);
        Logger.d(context,"Pusher", "Subscribing to event: " + eventUid);
        Logger.d(context,"Pusher", "Subscribing to eventOrder: " + eventOrder);
        Logger.d(context,"Pusher", "Subscribing to eventStatus: " + eventTransactionStatus);
        Logger.d(context,"Pusher", "Subscribing to eventCanceled: " + eventCanceled);
//        Logger.d(context,"Pusher", "Subscribing to orderResponseEvent: " + orderResponseEvent);

        // Обработка события получения номера заказа после регистрации на сервере и определения нал/безнал для выбора способа опроса статусов
//        channel.bind(eventUid, event -> {
        bindEvent(eventUid, event -> {
            Logger.d(context,"Pusher", "Received event: " + event.toString());
            try {
                JSONObject eventData = new JSONObject(event.getData());
                String orderUid = eventData.getString("order_uid");
                String  paySystemStatus;
                if (eventData.has("paySystemStatus")) {
                    paySystemStatus = eventData.getString("paySystemStatus");
                } else {
                    paySystemStatus = "nal_payment";
                }
                Logger.d(context,"Pusher", "Order UID: " + orderUid);
                Logger.d(context,"Pusher", "paySystemStatus: " + paySystemStatus);
                // Переключаемся на главный поток для обновления UI и переменной
                new Handler(Looper.getMainLooper()).post(() -> {
                    MainActivity.uid = orderUid;

                    MainActivity.paySystemStatus = paySystemStatus;

//                    if (FinishSeparateFragment.btn_cancel_order != null ) {
//                        FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
//                        FinishSeparateFragment.btn_cancel_order.setEnabled(true);
//                        FinishSeparateFragment.btn_cancel_order.setClickable(true);
//                    } else {
//                        Logger.e(context,"Pusher", "btn_cancel_order is null!");
//                    }
                });

            } catch (JSONException e) {
                Logger.e(context,"Pusher", "JSON Parsing error" +  e);
            }
        });

        bindEvent(eventTransactionStatus, event -> {
            Logger.d(context,"Pusher", "Received event: " + event.toString());

            try {
                JSONObject eventData = new JSONObject(event.getData());
                String uid = eventData.getString("uid");
                String transactionStatus = eventData.getString("transactionStatus");
                Logger.d(context,"Pusher eventTransactionStatus", "Parsed uid: " + uid);
                Logger.d(context,"Pusher eventTransactionStatus", "Parsed Main uid: " + MainActivity.uid);
                Logger.d(context,"Pusher eventTransactionStatus", "Parsed transactionStatus: " + transactionStatus);

                if(Objects.equals(MainActivity.uid, uid)) {
                    // Проверка на null перед переключением на главный поток
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Log.d("Pusher eventTransactionStatus", "Updating UI with status: " + transactionStatus);

                        // Установка начального статуса транзакции
                        viewModel.setTransactionStatus(transactionStatus);
                        Logger.d(context,"Pusher eventTransactionStatus", "Initial transaction status set: " + transactionStatus);

                        // Проверка UI элемента перед взаимодействием
                        if (FinishSeparateFragment.btn_cancel_order != null) {
                            FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
                            FinishSeparateFragment.btn_cancel_order.setEnabled(true);
                            FinishSeparateFragment.btn_cancel_order.setClickable(true);
                            Logger.d(context,"Pusher eventTransactionStatus", "Cancel button enabled successfully");
                        } else {
                            Logger.e(context,"Pusher eventTransactionStatus", "btn_cancel_order is null when updating status: " + transactionStatus);
                        }
                    });
                }


            } catch (JSONException e) {
                Logger.e(context,"Pusher", "JSON Parsing error for event: " + event.getData() +  e);
            } catch (Exception e) {
                Logger.e(context,"Pusher", "Unexpected error processing Pusher event" +  e);
            }
        });


        //Получение статуса холда
//        channel.bind(eventStartExecution, event -> {
//        bindEvent(eventStartExecution, event -> {
//            Logger.d(context,"Pusher", "Received event: " + event.toString());
//
//            try {
//                JSONObject eventData = new JSONObject(event.getData());
//                String eventStartExecution = eventData.getString("eventStartExecution");
//                Logger.d(context,"Pusher", "Parsed transactionStatus: " + eventStartExecution);
//
//                // Проверка на null перед переключением на главный поток
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    Log.d("Pusher", "Updating UI with status: " + eventStartExecution);
//
//                    // Проверка UI элемента перед взаимодействием
//                    if (FinishSeparateFragment.btn_cancel_order != null) {
//                        FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
//                        FinishSeparateFragment.btn_cancel_order.setEnabled(true);
//                        FinishSeparateFragment.btn_cancel_order.setClickable(true);
//                        Log.d("Pusher", "Cancel button enabled successfully");
//                    } else {
//                        Logger.e(context,"Pusher", "btn_cancel_order is null when updating status: " + eventStartExecution);
//                    }
//                });
//
//            } catch (JSONException e) {
//                Logger.e(context,"Pusher", "JSON Parsing error for event: " + event.getData() +  e);
//            } catch (Exception e) {
//                Logger.e(context,"Pusher", "Unexpected error processing Pusher event" +  e);
//            }
//        });

        // Получение статуса отмены заказа с сервера
//        channel.bind(eventCanceled, event -> {
        bindEvent(eventCanceled, event -> {
            Logger.d(context,"Pusher eventCanceled", "Received event: " + event.toString());

            try {
                JSONObject eventData = new JSONObject(event.getData());
                String canceled = eventData.getString("canceled");
                String uid = eventData.getString("uid");
                Logger.d(context,"Pusher eventCanceled", "canceled: " + canceled);
                Logger.d(context,"Pusher eventCanceled", "uid: " + uid);
                Logger.d(context,"Pusher eventCanceled", " MainActivity.uid: " +  MainActivity.uid);

                // Проверка, что uid существует и не null
                if (uid == null) {
                    Logger.d(context,"Pusher eventCanceled", "UID is null in orderResponse: " + eventData);
                } else if (MainActivity.uid != null && MainActivity.uid.equals(uid)) {
                    // Проверка на null перед переключением на главный поток
                    viewModel.setCanceledStatus(canceled);
                }
                } catch(JSONException e){
                    Logger.e(context,"Pusher eventCanceled", "JSON Parsing error for event: " + event.getData() +  e);
                } catch(Exception e){
                    Logger.e(context,"Pusher eventCanceled", "Unexpected error processing Pusher event" +  e);
                }

        });


        //Получение заказа из вилки с действием для отображения на фнинишной
//        channel.bind(orderResponseEvent, event -> {
//        bindEvent(orderResponseEvent, event -> {
//            Logger.d(context,"Pusher 1111 orderResponseEvent", "Received orderResponseEvent: " + event.toString());
//
//            try {
//                Gson gson = new Gson();
//
//                // Сначала распарсим `event.getData()` как строку
//                String jsonString = gson.fromJson(event.getData(), String.class);
//
//                // Теперь распарсим результат в объект OrderResponse
//                MainActivity.orderResponse = gson.fromJson(jsonString, OrderResponse.class);
//
//                if(MainActivity.orderResponse != null) {
//                    String uid = MainActivity.orderResponse.getUid();
//                    String action = MainActivity.orderResponse.getAction();
//
//
//                    Logger.d(context,"Pusher 1111 uid", "Received uid: " + uid);
//                    Logger.d(context,"Pusher 1111 uid", "MainActivity.uid: " + MainActivity.uid);
//                    Logger.d(context,"Pusher 1111 action", "Received action: " + action);
//
//                    // Проверка, что uid существует и не null
//                    if (uid == null) {
//                        Log.w("Pusher 1111 ", "UID is null in orderResponse: " + jsonString);
//                    } else if (MainActivity.uid != null && MainActivity.uid.equals(uid)) {
//
//                        EventBus.getDefault().post(new OrderResponseEvent(MainActivity.orderResponse));
////                        viewModel.updateOrderResponse(MainActivity.orderResponse);
//          //              new Handler(Looper.getMainLooper()).post(() -> {
//                            Logger.d(context,"Pusher 1111 orderResponseEvent", "Updating UI with orderResponse");
//
//                            if (FinishSeparateFragment.btn_cancel_order != null) {
//                                FinishSeparateFragment.btn_cancel_order.setVisibility(View.VISIBLE);
//                                FinishSeparateFragment.btn_cancel_order.setEnabled(true);
//                                FinishSeparateFragment.btn_cancel_order.setClickable(true);
//                            } else {
//                                Logger.e(context,"Pusher 1111 ", "btn_cancel_order is null!");
//                            }
////                        });
//                    } else {
//                        Logger.d(context,"Pusher 1111 ", "UIDs do not match or MainActivity.uid is null. MainActivity.uid: " + MainActivity.uid + ", Response uid: " + uid);
//                    }
//                }
//
//
//            } catch (JsonSyntaxException e) {
//                Logger.e(context,"Pusher 1111 ", "JSON Parsing error for event: " + event.getData() +  e);
//            } catch (Exception e) {
//                Logger.e(context,"Pusher 1111 ", "Unexpected error processing Pusher event" +  e);
//            }
//
//        });

        // Получение заказа с сервера после регистрации
//        channel.bind(eventOrder, event -> {
        bindEvent(eventOrder, event -> {
            Logger.d(context,"Pusher", "Received eventOrder: " + event.toString());
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
                    Logger.d(context,"Pusher", "Event Values: " + eventValues.toString());

            } catch (JSONException e) {
                // Логируем ошибку при парсинге JSON
                Logger.e(context,"Pusher", "JSON Parsing error" +  e);

                // Добавляем ошибку в Map
                Map<String, String> errorValues = new HashMap<>();
                errorValues.put("order_cost", "0");
                errorValues.put("message", "JSON Parsing error");
                Logger.e(context,"Pusher", "Error Values: " + errorValues.toString());
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
