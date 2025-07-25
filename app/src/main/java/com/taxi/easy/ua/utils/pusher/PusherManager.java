package com.taxi.easy.ua.utils.pusher;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.navigation.NavOptions;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.finish.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PusherManager {
    private static final String PUSHER_APP_KEY = "a10fb0bff91153d35f36"; // Ваш ключ
    private static final String PUSHER_CLUSTER = "mt1"; // Ваш кластер
    private static final String CHANNEL_NAME = "teal-towel-48"; // Канал
    private static final String TAG = "PusherManager";

    private final String eventUid;
    private final String eventUidDouble;
    private final String eventOrder;
    private final String eventAutoOrder;
    private final String eventTransactionStatus;
    private final String eventCanceled;
    private final String eventBlackUserStatus;
    private final String eventOrderCost;
//    private final String orderResponseEvent;
//    private final String eventStartExecution;
    private static Pusher pusher = null;
    private boolean isSubscribed = false;
    Channel channel;
   Activity context;
    private final ExecutionStatusViewModel viewModel;

    private final Set<String> boundEvents = new HashSet<>();
    public PusherManager(String eventSuffix, String userEmail, Activity context, ExecutionStatusViewModel viewModel) {
        this.eventUid = "order-status-updated-" + eventSuffix + "-" + userEmail;
        this.eventUidDouble = "orderDouble-status-updated-" + eventSuffix + "-" + userEmail;
        this.eventOrder = "order-" + eventSuffix + "-" + userEmail;
        this.eventAutoOrder = "orderAuto-" + eventSuffix + "-" + userEmail;
        this.eventTransactionStatus = "transactionStatus-" + eventSuffix + "-" + userEmail;
        this.eventCanceled = "eventCanceled-" + eventSuffix + "-" + userEmail;
        this.eventBlackUserStatus = "black-user-status--" + userEmail;
        this.eventOrderCost = "order-cost-" + eventSuffix + "-" + userEmail;
//        this.orderResponseEvent = "orderResponseEvent-" + eventSuffix + "-" + userEmail;\
        this.context = new WeakReference<>(context).get();
//        this.context = context;
//        this.eventStartExecution = "orderStartExecution-" + eventSuffix + "-" + userEmail;

        PusherOptions options = new PusherOptions();
        options.setCluster(PUSHER_CLUSTER);
// Получение ViewModel из области видимости Activity
        this.viewModel = viewModel;
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
                FirebaseCrashlytics.getInstance().recordException(e);
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
        Logger.d(context,"Pusher", "Subscribing to eventBlackUserStatus: " + eventBlackUserStatus);
//        Logger.d(context,"Pusher", "Subscribing to orderResponseEvent: " + orderResponseEvent);

        // Обработка события получения номера заказа после регистрации на сервере и определения нал/безнал для выбора способа опроса статусов
//        channel.bind(eventUid, event -> {
        bindEvent(eventUid, event -> {
            Logger.d(context,"UID 11123", "Received event: " + event.toString());
            try {
                JSONObject eventData = new JSONObject(event.getData());
                String orderUid = eventData.getString("order_uid");
                String  paySystemStatus;
                if (eventData.has("paySystemStatus")) {
                    paySystemStatus = eventData.getString("paySystemStatus");
                } else {
                    paySystemStatus = "nal_payment";
                }
                Logger.d(context,"UID 11123", "Order UID: " + orderUid);
                Logger.d(context,"UID 11123", "paySystemStatus: " + paySystemStatus);
                // Переключаемся на главный поток для обновления UI и переменной
                new Handler(Looper.getMainLooper()).post(() -> {

                    // Update ViewModel
                    viewModel.updateUid(orderUid);
                    viewModel.updatePaySystemStatus(paySystemStatus);

                });

            } catch (JSONException e) {
                Logger.e(context,"Pusher", "JSON Parsing error" +  e);
            }
        });

        bindEvent(eventUidDouble, event -> {
            Logger.d(context,"Pusher Double", "Received eventUidDouble: " + event.toString());
            try {
                JSONObject eventData = new JSONObject(event.getData());
                String orderUid = eventData.getString("order_uid");
                String  paySystemStatus;
                if (eventData.has("paySystemStatus")) {
                    paySystemStatus = eventData.getString("paySystemStatus");
                } else {
                    paySystemStatus = "nal_payment";
                }
                Logger.d(context,"Pusher Double", "Order UID Double: " + orderUid);
                Logger.d(context,"Pusher Double", "paySystemStatus: " + paySystemStatus);
                // Переключаемся на главный поток для обновления UI и переменной
                new Handler(Looper.getMainLooper()).post(() -> {

                    MainActivity.uid_Double = orderUid;
                    Logger.d(context,"Pusher Double", "MainActivity.uid_Double: " + MainActivity.uid_Double);

                    MainActivity.paySystemStatus = paySystemStatus;

                });

            } catch (JSONException e) {
                Logger.e(context,"Pusher Double", "JSON Parsing error" +  e);
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
                        viewModel.setCancelStatus(true);
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
                if (MainActivity.uid != null && MainActivity.uid.equals(uid)) {
                    // Проверка на null перед переключением на главный поток
                    viewModel.setCanceledStatus(canceled);
                }
                } catch(JSONException e){
                    Logger.e(context,"Pusher eventCanceled", "JSON Parsing error for event: " + event.getData() +  e);
                } catch(Exception e){
                    Logger.e(context,"Pusher eventCanceled", "Unexpected error processing Pusher event" +  e);
                }

        });
        // Получение стоимости
//        channel.bind(eventCanceled, event -> {
        bindEvent(eventOrderCost, event -> {
            Logger.d(context,"Pusher eventOrderCost", "Received event: " + event.toString());

            try {
                JSONObject eventData = new JSONObject(event.getData());
                String order_cost = eventData.getString("order_cost");
                Logger.d(context,"Pusher eventOrderCost", "order_cost: " + order_cost);

                Map<String, String> eventValues = new HashMap<>();
                // Добавляем данные в Map
                eventValues.put("order_cost", eventData.optString("order_cost", "0"));
                eventValues.put("Message", eventData.optString("Message", ""));

                MainActivity.costMap = eventValues;
                sharedPreferencesHelperMain.saveValue("order_cost", eventData.optString("order_cost", "0"));

            } catch(JSONException e){
                    Logger.e(context,"Pusher eventOrderCost", "JSON Parsing error for event: " + event.getData() +  e);
            }

        });

        bindEvent(eventBlackUserStatus, event -> {
            Logger.d(context,"Pusher eventBlackUserStatus", "Received event: " + event.toString());

            try {
                JSONObject eventData = new JSONObject(event.getData());
                String active = eventData.getString("active");
                String email = eventData.getString("email");

                Logger.d(context,"Pusher eventBlackUserStatus", "canceled: " + active);
                Logger.d(context,"Pusher eventBlackUserStatus", "email: " + email);

                String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                Logger.d(context,"Pusher eventCanceled", "userEmail: " + userEmail);

                if (email.equals(userEmail)) {
                    sharedPreferencesHelperMain.saveValue("verifyUserOrder", active);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_visicom, true)
                            .build());
                });

            } catch(JSONException e){
                Logger.e(context,"Pusher eventBlackUserStatus", "JSON Parsing error for event: " + event.getData() +  e);
            } catch(Exception e){
                Logger.e(context,"Pusher eventBlackUserStatus", "Unexpected error processing Pusher event" +  e);
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

        bindEvent(eventAutoOrder, event -> {
            Logger.d(context,"Pusher", "Received eventAutoOrder: " + event.toString());

//            if (MainActivity.currentNavDestination != R.id.nav_finish_separate) {
                try {

                    // Преобразуем данные события в JSONObject
                    JSONObject eventData = new JSONObject(event.getData());

                    // Создаём Map для хранения данных
                    Map<String, String> eventValues = new HashMap<>();
                    // Добавляем данные в Map

                    eventValues.put("dispatching_order_uid", eventData.optString("dispatching_order_uid", "null"));
                    eventValues.put("order_cost", eventData.optString("order_cost", "0"));
                    eventValues.put("routefrom", eventData.optString("routefrom", "null"));
                    eventValues.put("routefromnumber", eventData.optString("routefromnumber", "null"));
                    eventValues.put("routeto", eventData.optString("routeto", "null"));
                    eventValues.put("to_number", eventData.optString("to_number", "null"));

                    eventValues.put("pay_method", eventData.optString("pay_method", "nal_payment"));
                    eventValues.put("orderWeb", eventData.optString("order_cost", "0"));

//                eventValues.put("currency", eventData.optString("currency", "null"));
                    eventValues.put("required_time", eventData.optString("required_time", ""));
                    eventValues.put("flexible_tariff_name", eventData.optString("flexible_tariff_name", "null"));
                    eventValues.put("comment_info", eventData.optString("comment_info", ""));
                    eventValues.put("extra_charge_codes", eventData.optString("extra_charge_codes", ""));

                    // Добавляем дополнительные поля, если они существуют

                    String dispatchingOrderUidDouble = eventData.optString("dispatching_order_uid_Double", " ");
                    eventValues.put("dispatching_order_uid_Double", dispatchingOrderUidDouble.equals(" ") ? " " : dispatchingOrderUidDouble);

                    Logger.d(context,"Pusher", "Received eventAutoOrder: " + eventValues.toString());

                    startFinishPage(eventValues);

                } catch (JSONException e) {
                    // Логируем ошибку при парсинге JSON
                    Logger.e(context,"Pusher", "JSON Parsing error" +  e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
//            }
        });

    }


    private void startFinishPage(Map<String, String> sendUrlMap) throws ParseException {
    if (MainActivity.currentNavDestination == R.id.nav_finish_separate) {
        String paySystemStatus = "nal_payment";
        String orderUid = sendUrlMap.get("dispatching_order_uid");;

        Logger.e(context,"startFinishPage", "paySystemStatus" +  paySystemStatus);
        Logger.e(context,"startFinishPage", "orderUid" +  orderUid);
        new Handler(Looper.getMainLooper()).post(() -> {
            if(orderUid != null){
                viewModel.updateUid(orderUid);
                viewModel.updatePaySystemStatus(paySystemStatus);
            }

            viewModel.setStatusNalUpdate(false);
        });
    } else {
        String to_name;

        if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
            to_name = context.getString(R.string.on_city_tv);
            Logger.d(context, "startFinishPage", "startFinishPage: to_name 1 " + to_name);

        } else {

            if(Objects.equals(sendUrlMap.get("routeto"), "Точка на карте")) {
                to_name = context.getString(R.string.end_point_marker);
            } else {
                to_name = sendUrlMap.get("routeto") + " " + sendUrlMap.get("to_number");
            }
            Logger.d(context, "startFinishPage", "startFinishPage: to_name 2 " + to_name);
        }
        Logger.d(context, "startFinishPage", "startFinishPage: to_name 3" + to_name);
        String to_name_local = to_name;
        if(to_name.contains("по місту")
                ||to_name.contains("по городу")
                || to_name.contains("around the city")
        ) {
            to_name_local = context.getString(R.string.on_city_tv);
        }
        Logger.d(context, "startFinishPage", "startFinishPage: to_name 4" + to_name_local);
        String pay_method_message = context.getString(R.string.pay_method_message_main);
        switch (Objects.requireNonNull(sendUrlMap.get("pay_method"))) {
            case "bonus_payment":
                pay_method_message += " " + context.getString(R.string.pay_method_message_bonus);
                break;
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
            case "wfp_payment":
                pay_method_message += " " + context.getString(R.string.pay_method_message_card);
                break;
            default:
                pay_method_message += " " + context.getString(R.string.pay_method_message_nal);
        }

        String routeFrom = cleanString(sendUrlMap.get("routefrom") + " " +sendUrlMap.get("routefromnumber"));
        String toMessage = cleanString(context.getString(R.string.to_message));
        String toNameLocal = cleanString(to_name_local);
        String orderWeb = cleanString(Objects.requireNonNull(sendUrlMap.get("orderWeb")));
        String uah = cleanString(context.getString(R.string.UAH));
        String payMethodMessage = cleanString(pay_method_message);
        String required_time = sendUrlMap.get("required_time");
        Logger.d(context, "startFinishPage", "orderFinished: required_time " + required_time);


        if (required_time != null && !required_time.contains("1970-01-01")) {
            try {
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                // Преобразуем строку required_time в Date
                Date date = inputFormat.parse(required_time);

                // Преобразуем Date в строку нужного формата
                assert date != null;
                required_time = " " + context.getString(R.string.time_order)  + " " +  outputFormat.format(date)  + ".";

            } catch (ParseException e) {
                required_time = ""; // Если ошибка парсинга, задаём пустое значение
            }
        } else {
            required_time = "";
        }

        String messageResult =
                routeFrom + " " +
                        toMessage + " " +
                        toNameLocal + ". " +
                        required_time;
        String messagePayment = orderWeb + " " + uah + " " + payMethodMessage;
        Logger.d(context, TAG, "messageResult: " + messageResult);
        Logger.d(context, TAG, "messagePayment: " + messagePayment);

        String messageFondy = context.getString(R.string.fondy_message) + " " +
                sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                to_name_local + ".";
        Logger.d(context, TAG, "startFinishPage: messageResult " + messageResult);
        Logger.d(context, TAG, "startFinishPage: to_name " + to_name);

        Logger.d(context, TAG, "orderWeb: " + orderWeb);
        Logger.d(context, TAG, "uah: " + uah);
        Logger.d(context, TAG, "payMethodMessage: " + payMethodMessage);


        Bundle bundle = new Bundle();
        bundle.putString("messageResult_key", messageResult);
        bundle.putString("messagePay_key", messagePayment);
        bundle.putString("messageFondy_key", messageFondy);
        bundle.putString("messageCost_key", Objects.requireNonNull(sendUrlMap.get("orderWeb")));
        bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
        bundle.putString("card_payment_key", "no");
        bundle.putString("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
        bundle.putString("dispatching_order_uid_Double", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid_Double")));
        viewModel.setStatusNalUpdate(true); //наюлюдение за опросом статусом нала
        new Handler(Looper.getMainLooper()).post(() -> {

            // Выполняем навигацию в главном потоке
            MainActivity.navController.navigate(
                    R.id.nav_finish_separate,
                    bundle,
                    new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_visicom, true)
                            .build()
            );
        });
    }



    }
    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ").replaceAll("\\s{2,}$", " ");
    }

    @SuppressLint("Range")
    private static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            String str;
            do {
                str = "";
                for (String cn : c.getColumnNames()) {
                    str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                    list.add(c.getString(c.getColumnIndex(cn)));

                }

            } while (c.moveToNext());
        }
        database.close();
        c.close();
        return list;
    }
    // Отключение
    public void disconnect() {
        if (pusher != null) {
            pusher.disconnect();
        }
    }

}
