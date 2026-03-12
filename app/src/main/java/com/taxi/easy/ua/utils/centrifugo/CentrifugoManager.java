package com.taxi.easy.ua.utils.centrifugo;

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
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.pusher.events.TransactionStatusEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.centrifugal.centrifuge.Client;
import io.github.centrifugal.centrifuge.ConnectedEvent;
import io.github.centrifugal.centrifuge.ConnectingEvent;
import io.github.centrifugal.centrifuge.DisconnectedEvent;
import io.github.centrifugal.centrifuge.ErrorEvent;
import io.github.centrifugal.centrifuge.EventListener;
import io.github.centrifugal.centrifuge.Options;
import io.github.centrifugal.centrifuge.PublicationEvent;
import io.github.centrifugal.centrifuge.SubscribedEvent;
import io.github.centrifugal.centrifuge.SubscribingEvent;
import io.github.centrifugal.centrifuge.Subscription;
import io.github.centrifugal.centrifuge.SubscriptionEventListener;
import io.github.centrifugal.centrifuge.UnsubscribedEvent;

/**
 * Управляет подключением к Centrifugo и обработкой событий в реальном времени
 * Версия с официальным Centrifuge SDK
 */
public class CentrifugoManager {
    private static final String CENTRIFUGO_URL = "wss://t.easy-order-taxi.site/connection/websocket";
    private static final String CHANNEL_NAME = "teal-towel-48";
    private static final String TAG = "CentrifugoManager";
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int MAX_PROCESSED_EVENTS = 100;

    private final String eventUid;
    private final String eventUidDouble;
    private final String eventOrder;
    private final String eventAutoOrder;
    private final String eventTransactionStatus;
    private final String eventCanceled;
    private final String eventBlackUserStatus;
    private final String eventOrderCost;

    private Client client;
    private Subscription subscription;
    private final WeakReference<Activity> activityRef;
    private final ExecutionStatusViewModel viewModel;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    private final CentrifugoHandler mainHandler;

    private String lastProcessedCost = "";
    private boolean isShuttingDown = false;
    private int reconnectAttempts = 0;
    // Флаг для отслеживания состояния подключения
    private boolean isConnectedFlag = false;
    private final String connectionToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiaWF0IjoxNzczMTM2NjAxfQ.emOb9qlpplxbLIhN_rHw5ADRPCcXnJ_eNZ10wTEWidA";

    /**
     * Безопасный Handler
     */
    private static class CentrifugoHandler extends Handler {
        private final WeakReference<CentrifugoManager> managerRef;

        CentrifugoHandler(CentrifugoManager manager) {
            super(Looper.getMainLooper());
            this.managerRef = new WeakReference<>(manager);
        }

        public void postSafe(Runnable r) {
            post(() -> {
                CentrifugoManager manager = managerRef.get();
                if (manager != null && manager.isContextValid()) {
                    r.run();
                }
            });
        }
    }

    public CentrifugoManager(String eventSuffix, String userEmail, Activity context,
                             ExecutionStatusViewModel viewModel) {
        this.eventUid = "order_uid_new-" + eventSuffix + "-" + userEmail;
        this.eventUidDouble = "orderDouble-status-updated-" + eventSuffix + "-" + userEmail;
        this.eventOrder = "order-" + eventSuffix + "-" + userEmail;
        this.eventAutoOrder = "orderAuto-" + eventSuffix + "-" + userEmail;
        this.eventTransactionStatus = "transactionStatus-" + eventSuffix + "-" + userEmail;
        this.eventCanceled = "eventCanceled-" + eventSuffix + "-" + userEmail;
        this.eventBlackUserStatus = "black-user-status--" + userEmail;
        this.eventOrderCost = "order-cost-" + eventSuffix + "-" + userEmail;

        this.activityRef = new WeakReference<>(context);
        this.viewModel = viewModel;
        this.mainHandler = new CentrifugoHandler(this);

        initCentrifugeClient();
    }

    /**
     * Инициализация Centrifuge клиента
     */
    private void initCentrifugeClient() {
        Options options = new Options();
        options.setToken(connectionToken);

        // Создаем клиент с тремя параметрами: URL, options, слушатель
        client = new Client(CENTRIFUGO_URL, options, new EventListener() {
            @Override
            public void onConnected(Client client, ConnectedEvent event) {
                Log.d(TAG, "✅ Connected to Centrifugo");
                logToContext(TAG, "Connected successfully");
                reconnectAttempts = 0;
                isConnectedFlag = true;
                subscribeToChannel();
            }

            @Override
            public void onConnecting(Client client, ConnectingEvent event) {
                Log.d(TAG, "🔄 Connecting to Centrifugo...");
                logToContext(TAG, "Connecting...");
            }

            @Override
            public void onDisconnected(Client client, DisconnectedEvent event) {
                Log.d(TAG, "❌ Disconnected from Centrifugo: " + event.getReason());
                logToContext(TAG, "Disconnected: " + event.getReason());
                isConnectedFlag = false;
                attemptReconnection();
            }

            @Override
            public void onError(Client client, ErrorEvent event) {
                String errorMsg = event.toString();
                Log.e(TAG, "❌ Client error: " + errorMsg);
                FirebaseCrashlytics.getInstance().log("Centrifugo error: " + errorMsg);
            }
        });
    }

    /**
     * Подключение к Centrifugo
     */
    public void connect() {
        if (!isContextValid()) {
            Log.w(TAG, "Context is invalid, skipping connection");
            return;
        }

        if (isShuttingDown) {
            return;
        }

        resetReconnectionAttempts();
        logToContext(TAG, "Connecting to Centrifugo...");

        try {
            client.connect();
        } catch (Exception e) {
            logErrorToContext(TAG, "Connection error", e);
            attemptReconnection();
        }
    }

    /**
     * Попытка переподключения с экспоненциальной задержкой
     */
    private void attemptReconnection() {
        if (isShuttingDown || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnection attempts reached or shutting down");
            return;
        }

        reconnectAttempts++;

        // Экспоненциальная задержка
        long delay = (long) (RECONNECT_DELAY_MS * Math.pow(1.5, reconnectAttempts - 1));
        delay = Math.min(delay, 30000); // Максимум 30 секунд

        logToContext(TAG, "Scheduling reconnection attempt " + reconnectAttempts +
                " in " + delay + "ms");

        mainHandler.postDelayed(() -> {
            if (!isShuttingDown && isContextValid()) {
                logToContext(TAG, "Attempting reconnection...");
                connect();
            }
        }, delay);
    }

    /**
     * Сброс счетчика попыток переподключения
     */
    private void resetReconnectionAttempts() {
        reconnectAttempts = 0;
    }


    /**
     * Подписка на канал
     */
    public void subscribeToChannel() {
        try {
            // Проверяем, существует ли уже подписка
            if (subscription != null) {
                // Если подписка уже есть и она активна, просто возвращаемся
                Log.d(TAG, "Subscription already exists, reusing");
                return;
            }

            // Создаем новую подписку
            subscription = client.newSubscription(CHANNEL_NAME, new SubscriptionEventListener() {
                @Override
                public void onPublication(Subscription subscription, PublicationEvent event) {
                    try {
                        String data = new String(event.getData(), StandardCharsets.UTF_8);
                        Log.d(TAG, "📩 Publication received: " + data);
                        handlePublication(data);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing publication", e);
                    }
                }

                @Override
                public void onSubscribed(Subscription subscription, SubscribedEvent event) {
                    Log.d(TAG, "✅ Subscribed to channel " + CHANNEL_NAME);
                    logToContext(TAG, "Subscribed to channel");
                }

                @Override
                public void onSubscribing(Subscription subscription, SubscribingEvent event) {
                    Log.d(TAG, "🔄 Subscribing to channel...");
                }

                @Override
                public void onUnsubscribed(Subscription subscription, UnsubscribedEvent event) {
                    Log.d(TAG, "❌ Unsubscribed from channel: " + event.getReason());
                    subscription = null; // Сбрасываем при отписке
                }
            });

            subscription.subscribe();

        } catch (Exception e) {
            Log.e(TAG, "Error creating subscription", e);
            logErrorToContext(TAG, "Error subscribing", e);
        }
    }

    private void handlePublication(String data) {
        logToContext(TAG, "Received event: " + data);

        try {
            JSONObject jsonObject = new JSONObject(data);

            // Получаем название события из поля "event"
            String eventName = jsonObject.optString("event", "");

            // ДИАГНОСТИКА: логируем все события
            Log.d(TAG, "🔍 RAW EVENT: " + eventName);

            // Проверяем, содержит ли событие transactionStatus
            if (eventName.contains("transactionStatus")) {
                Log.d(TAG, "⚠️ TRANSACTION EVENT DETECTED: " + eventName);
            }

            // ФИЛЬТРАЦИЯ: проверяем, относится ли событие к этому клиенту
            if (!eventName.isEmpty()) {
                // Проверяем, содержит ли eventName наши параметры
                if (!eventName.contains(eventUid) &&
                        !eventName.contains(eventUidDouble) &&
                        !eventName.contains(eventOrder) &&
                        !eventName.contains(eventAutoOrder) &&
                        !eventName.contains(eventTransactionStatus) &&
                        !eventName.contains(eventCanceled) &&
                        !eventName.contains(eventBlackUserStatus) &&
                        !eventName.contains(eventOrderCost)) {
                    // Если событие не относится к этому клиенту - игнорируем
                    Log.d(TAG, "Event ignored - not for this client: " + eventName);
                    return;
                }
                Log.d(TAG, "Event accepted for this client: " + eventName);
            }

            // Создаем уникальный ключ для события
            String uniqueKey = "pub_" + data.hashCode();

            // Проверка на дубликаты
            if (processedEventIds.contains(uniqueKey)) {
                Log.d(TAG, "Duplicate event ignored: " + uniqueKey);
                return;
            }

            processedEventIds.add(uniqueKey);
            // Ограничиваем размер множества
            if (processedEventIds.size() > MAX_PROCESSED_EVENTS) {
                processedEventIds.clear();
            }

            // Получаем данные из поля "data" если оно есть
            JSONObject eventData = jsonObject;
            if (jsonObject.has("data")) {
                eventData = jsonObject.getJSONObject("data");
            }

            // ДИАГНОСТИКА: проверяем наличие полей transactionStatus
            if (eventData.has("transactionStatus")) {
                Log.d(TAG, "🎯 FOUND transactionStatus in data!");
            }

            // Определяем тип события по полям в данных
            if (eventData.has("order_uid_new")) {
                handleUidEvent(eventData);
            } else if (eventData.has("orderDouble")) {
                handleUidDoubleEvent(eventData);
            } else if (eventData.has("transactionStatus")) {
                Log.d(TAG, "➡️ Calling handleTransactionStatusEvent");
                handleTransactionStatusEvent(eventData);
            } else if (eventData.has("canceled")) {
                handleCanceledEvent(eventData);
            } else if (eventData.has("order_cost")) {
                handleOrderCostEvent(eventData);
            } else if (eventData.has("active") && eventData.has("email")) {
                handleBlackUserStatusEvent(eventData);
            } else if (eventData.has("from_lat") && eventData.has("dispatching_order_uid")) {
                handleOrderEvent(eventData);
            } else if (eventData.has("dispatching_order_uid") && eventData.has("pay_method")) {
                handleAutoOrderEvent(eventData);
            }

        } catch (JSONException e) {
            logErrorToContext(TAG, "JSON Parsing error", e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /**
     * Обработка UID события
     */
    private void handleUidEvent(JSONObject json) throws JSONException {
        String TAG = "handleUidEvent";
        Log.d(TAG, "========== handleUidEvent START ==========");

        try {
            // Логируем весь JSON
            Log.d(TAG, "JSON in handleUidEvent: " + json.toString());

            // Проверяем наличие поля
            if (!json.has("order_uid_new")) {
                Log.e(TAG, "❌ CRITICAL: order_uid_new field missing!");
                Log.d(TAG, "Available keys: " + json.keys().toString());
                return;
            }

            String orderUid = json.getString("order_uid_new");
            String paySystemStatus = json.optString("paySystemStatus", "nal_payment");

            Log.d(TAG, "✅ Parsed - UID: " + orderUid + ", paySystemStatus: " + paySystemStatus);
            Log.d(TAG, "Current MainActivity.uid: " + MainActivity.uid);
            Log.d(TAG, "Context valid: " + isContextValid());

            // Пробуем обновить напрямую
            if (isContextValid()) {
                Activity activity = activityRef.get();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "🟢 Updating ViewModel on UI thread");
                            viewModel.updateUid(orderUid);
                            viewModel.updatePaySystemStatus(paySystemStatus);
                            Log.d(TAG, "✅ ViewModel updated, new value should be: " + orderUid);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ UI thread update failed", e);
                        }
                    });
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "❌ JSONException in handleUidEvent", e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in handleUidEvent", e);
        }

        Log.d(TAG, "========== handleUidEvent END ==========");
    }

    /**
     * Обработка UID Double события
     */
    private void handleUidDoubleEvent(JSONObject json) throws JSONException {
        String orderDouble = json.getString("orderDouble");
        String paySystemStatus = json.optString("paySystemStatus", "nal_payment");

        logToContext("Centrifugo Double", "Order UID Double: " + orderDouble);

        mainHandler.postSafe(() -> {
            MainActivity.uid_Double = orderDouble;
            MainActivity.paySystemStatus = paySystemStatus;
        });
    }

    /**
     * Обработка статуса транзакции
     */
    private void handleTransactionStatusEvent(JSONObject json) throws JSONException {
        String TAG = "handleTransactionStatusEvent";
        Log.d(TAG, "========== handleTransactionStatusEvent START ==========");

        try {
            String uid = json.getString("uid");
            String transactionStatus = json.getString("transactionStatus");

            Log.d(TAG, "📦 Transaction event data: " + json.toString());
            Log.d(TAG, "Event UID: " + uid);
            Log.d(TAG, "Event Status: " + transactionStatus);

            // Получаем UID из ViewModel
            String viewModelUid = viewModel.getUid().getValue();
            Log.d(TAG, "ViewModel UID: " + viewModelUid);
            Log.d(TAG, "MainActivity.uid: " + MainActivity.uid);

            // Проверяем, жив ли еще контекст
            Log.d(TAG, "Context valid: " + isContextValid());

            // Проверяем, есть ли совпадение
            if (Objects.equals(viewModelUid, uid)) {
                Log.d(TAG, "✅ UID MATCH with ViewModel!");

                // Отправляем событие через EventBus
                EventBus.getDefault().post(new TransactionStatusEvent(transactionStatus));
                Log.d(TAG, "📢 EventBus post sent");

                if (isContextValid()) {
                    mainHandler.postSafe(() -> {
                        try {
                            viewModel.setTransactionStatus(transactionStatus);
                            Log.d(TAG, "✅ Transaction status set in ViewModel: " + transactionStatus);


                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error in postSafe", e);
                        }
                    });
                }
            } else if (Objects.equals(MainActivity.uid, uid)) {
                Log.d(TAG, "✅ UID MATCH with MainActivity.uid!");

                EventBus.getDefault().post(new TransactionStatusEvent(transactionStatus));

                if (isContextValid()) {
                    mainHandler.postSafe(() -> {
                        viewModel.setTransactionStatus(transactionStatus);
                        Log.d(TAG, "✅ Transaction status set from MainActivity.uid");
                    });
                }
            } else {
                Log.d(TAG, "❌ UID MISMATCH:");
                Log.d(TAG, "   Event UID: " + uid);
                Log.d(TAG, "   ViewModel UID: " + viewModelUid);
                Log.d(TAG, "   MainActivity.uid: " + MainActivity.uid);

                // Сохраняем для отладки
                savePendingTransaction(uid, transactionStatus);
            }
        } catch (JSONException e) {
            Log.e(TAG, "❌ JSONException in handleTransactionStatusEvent", e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in handleTransactionStatusEvent", e);
        }

        Log.d(TAG, "========== handleTransactionStatusEvent END ==========");
    }

    // Временный метод для сохранения пропущенных транзакций
    private void savePendingTransaction(String uid, String status) {
        Log.d(TAG, "💾 Saving pending transaction - UID: " + uid + ", Status: " + status);
        sharedPreferencesHelperMain.saveValue("pending_transaction_uid", uid);
        sharedPreferencesHelperMain.saveValue("pending_transaction_status", status);
        sharedPreferencesHelperMain.saveValue("pending_transaction_time", String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Обработка отмены заказа
     */
    private void handleCanceledEvent(JSONObject json) throws JSONException {
        String canceled = json.getString("canceled");
        String uid = json.getString("uid");

        if (MainActivity.uid != null && MainActivity.uid.equals(uid) && isContextValid()) {
            viewModel.setCanceledStatus(canceled);
        }
    }

    /**
     * Обработка стоимости заказа
     */
    private void handleOrderCostEvent(JSONObject json) throws JSONException {
        String orderCost = json.optString("order_cost", "0");
        Log.d(TAG, "order_cost " + orderCost);
        if (orderCost.equals(lastProcessedCost)) {
            Log.d(TAG, "Duplicate cost ignored: " + orderCost);
            return;
        }

        lastProcessedCost = orderCost;

        mainHandler.postSafe(() -> {
            if (isContextValid()) {
                MainActivity.orderViewModel.setOrderCost(orderCost);
                sharedPreferencesHelperMain.saveValue("order_cost", orderCost);
            }
        });
    }

    /**
     * Обработка статуса черного списка пользователя
     */
    private void handleBlackUserStatusEvent(JSONObject json) throws JSONException {
        String active = json.getString("active");
        String email = json.getString("email");

        if (!isContextValid()) return;

        Context context = getContext();
        if (context == null) return;

        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        if (email.equals(userEmail)) {
            sharedPreferencesHelperMain.saveValue("verifyUserOrder", active);
        }

        mainHandler.postSafe(() -> {
            if (isContextValid()) {
                MainActivity.navController.navigate(
                        R.id.nav_visicom,
                        null,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build()
                );
            }
        });
    }

    /**
     * Обработка заказа
     */
    private void handleOrderEvent(JSONObject json) throws JSONException {
        Map<String, String> eventValues = new HashMap<>();
        eventValues.put("from_lat", json.optString("from_lat", "null"));
        eventValues.put("from_lng", json.optString("from_lng", "null"));
        eventValues.put("lat", json.optString("lat", "null"));
        eventValues.put("lng", json.optString("lng", "null"));
        eventValues.put("dispatching_order_uid", json.optString("dispatching_order_uid", "null"));
        eventValues.put("order_cost", json.optString("order_cost", "0"));
        eventValues.put("currency", json.optString("currency", "null"));
        eventValues.put("routefrom", json.optString("routefrom", "null"));
        eventValues.put("routefromnumber", json.optString("routefromnumber", "null"));
        eventValues.put("routeto", json.optString("routeto", "null"));
        eventValues.put("to_number", json.optString("to_number", "null"));
        eventValues.put("required_time", json.optString("required_time", ""));
        eventValues.put("flexible_tariff_name", json.optString("flexible_tariff_name", "null"));
        eventValues.put("comment_info", json.optString("comment_info", ""));
        eventValues.put("extra_charge_codes", json.optString("extra_charge_codes", ""));

        String dispatchingOrderUidDouble = json.optString("dispatching_order_uid_Double", " ");
        eventValues.put("dispatching_order_uid_Double",
                dispatchingOrderUidDouble.equals(" ") ? " " : dispatchingOrderUidDouble);

        VisicomFragment.sendUrlMap = eventValues;
    }

    /**
     * Обработка авто-заказа
     */
    private void handleAutoOrderEvent(JSONObject json) throws JSONException {
        Map<String, String> eventValues = new HashMap<>();
        eventValues.put("dispatching_order_uid", json.optString("dispatching_order_uid", "null"));
        eventValues.put("order_cost", json.optString("order_cost", "0"));
        eventValues.put("routefrom", json.optString("routefrom", "null"));
        eventValues.put("routefromnumber", json.optString("routefromnumber", "null"));
        eventValues.put("routeto", json.optString("routeto", "null"));
        eventValues.put("to_number", json.optString("to_number", "null"));
        eventValues.put("pay_method", json.optString("pay_method", "nal_payment"));
        eventValues.put("orderWeb", json.optString("order_cost", "0"));
        eventValues.put("required_time", json.optString("required_time", ""));
        eventValues.put("flexible_tariff_name", json.optString("flexible_tariff_name", "null"));
        eventValues.put("comment_info", json.optString("comment_info", ""));
        eventValues.put("extra_charge_codes", json.optString("extra_charge_codes", ""));

        String dispatchingOrderUidDouble = json.optString("dispatching_order_uid_Double", " ");
        eventValues.put("dispatching_order_uid_Double",
                dispatchingOrderUidDouble.equals(" ") ? " " : dispatchingOrderUidDouble);

        if (isContextValid()) {
            try {
                startFinishPage(eventValues);
            } catch (ParseException e) {
                logErrorToContext(TAG, "Error starting finish page", e);
            }
        }
    }

    /**
     * Запуск финишной страницы
     */
    private void startFinishPage(Map<String, String> sendUrlMap) throws ParseException {
        if (!isContextValid()) return;

        Activity activity = activityRef.get();
        if (activity == null) return;

        if (MainActivity.currentNavDestination == R.id.nav_finish_separate) {
            String paySystemStatus = "nal_payment";
            String orderUid = sendUrlMap.get("dispatching_order_uid");

            mainHandler.postSafe(() -> {
                if (orderUid != null) {
                    viewModel.updateUid(orderUid);
                    viewModel.updatePaySystemStatus(paySystemStatus);
                }
                viewModel.setStatusNalUpdate(false);
            });
        } else {
            String to_name = buildDestinationName(sendUrlMap, activity);
            String pay_method_message = buildPaymentMessage(sendUrlMap, activity);

            String routeFrom = cleanString(sendUrlMap.get("routefrom") + " " +
                    sendUrlMap.get("routefromnumber"));
            String toNameLocal = cleanString(to_name);
            String orderWeb = cleanString(Objects.requireNonNull(sendUrlMap.get("orderWeb")));
            String uah = cleanString(activity.getString(R.string.UAH));
            String required_time = formatRequiredTime(sendUrlMap.get("required_time"), activity);

            String messageResult = routeFrom + " " +
                    activity.getString(R.string.to_message) + " " +
                    toNameLocal + ". " + required_time;

            String messagePayment = orderWeb + " " + uah + " " + pay_method_message;
            String messageFondy = activity.getString(R.string.fondy_message) + " " +
                    sendUrlMap.get("routefrom") + " " +
                    activity.getString(R.string.to_message) + toNameLocal + ".";

            Bundle bundle = new Bundle();
            bundle.putString("messageResult_key", messageResult);
            bundle.putString("messagePay_key", messagePayment);
            bundle.putString("messageFondy_key", messageFondy);
            bundle.putString("messageCost_key", Objects.requireNonNull(sendUrlMap.get("orderWeb")));
            bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
            bundle.putString("card_payment_key", "no");
            bundle.putString("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
            bundle.putString("dispatching_order_uid_Double",
                    Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid_Double")));

            viewModel.setStatusNalUpdate(true);

            mainHandler.postSafe(() -> {
                if (isContextValid()) {
                    MainActivity.navController.navigate(
                            R.id.nav_finish_separate,
                            bundle,
                            new NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_visicom, true)
                                    .build()
                    );
                }
            });
        }
    }

    /**
     * Формирование названия назначения
     */
    private String buildDestinationName(Map<String, String> sendUrlMap, Activity activity) {
        String routeFrom = sendUrlMap.get("routefrom");
        String routeTo = sendUrlMap.get("routeto");
        String toNumber = sendUrlMap.get("to_number");

        if (Objects.equals(routeFrom, routeTo)) {
            return activity.getString(R.string.on_city_tv);
        }

        String toName;
        if (Objects.equals(routeTo, "Точка на карте")) {
            toName = activity.getString(R.string.end_point_marker);
        } else {
            toName = routeTo + " " + toNumber;
        }

        if (toName.contains("по місту") || toName.contains("по городу") ||
                toName.contains("around the city")) {
            return activity.getString(R.string.on_city_tv);
        }

        return toName;
    }

    /**
     * Формирование сообщения о способе оплаты
     */
    private String buildPaymentMessage(Map<String, String> sendUrlMap, Activity activity) {
        String baseMessage = activity.getString(R.string.pay_method_message_main);
        String payMethod = sendUrlMap.get("pay_method");

        if (payMethod == null) {
            return baseMessage + " " + activity.getString(R.string.pay_method_message_nal);
        }

        switch (payMethod) {
            case "bonus_payment":
                return baseMessage + " " + activity.getString(R.string.pay_method_message_bonus);
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
            case "wfp_payment":
                return baseMessage + " " + activity.getString(R.string.pay_method_message_card);
            default:
                return baseMessage + " " + activity.getString(R.string.pay_method_message_nal);
        }
    }

    /**
     * Форматирование времени заказа
     */
    private String formatRequiredTime(String requiredTime, Activity activity) {
        if (requiredTime == null || requiredTime.contains("1970-01-01")) {
            return "";
        }

        try {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

            Date date = inputFormat.parse(requiredTime);
            if (date != null) {
                return " " + activity.getString(R.string.time_order) + " " +
                        outputFormat.format(date) + ".";
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing required_time", e);
        }
        return "";
    }

    /**
     * Очистка строки от лишних пробелов
     */
    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Проверка валидности контекста
     */
    private boolean isContextValid() {
        Activity activity = activityRef != null ? activityRef.get() : null;
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    /**
     * Получение контекста
     */
    private Context getContext() {
        return activityRef != null ? activityRef.get() : null;
    }

    /**
     * Логирование в контекст
     */
    private void logToContext(String tag, String message) {
        Log.d(tag, message);
        Context context = getContext();
        if (context != null) {
            Logger.d(context, tag, message);
        }
    }

    /**
     * Логирование ошибки в контекст
     */
    private void logErrorToContext(String tag, String message, Exception e) {
        Log.e(tag, message, e);
        Context context = getContext();
        if (context != null) {
            Logger.e(context, tag, message + (e != null ? ": " + e.getMessage() : ""));
        }
    }

    /**
     * Чтение данных из SQLite
     */
    @SuppressLint("Range")
    private static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = null;
        Cursor c = null;

        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            c = database.query(table, null, null, null, null, null, null);

            if (c.moveToFirst()) {
                do {
                    for (String cn : c.getColumnNames()) {
                        list.add(c.getString(c.getColumnIndex(cn)));
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading database", e);
        } finally {
            if (c != null) {
                c.close();
            }
            if (database != null) {
                database.close();
            }
        }

        return list;
    }

    /**
     * Отключение от Centrifugo
     */

    public void disconnect() {
        isShuttingDown = true;

        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (Exception e) {
                Log.e(TAG, "Error unsubscribing", e);
            }
            subscription = null;
        }

        if (client != null) {
            try {
                client.disconnect();
                // Убираем client.close() если он не нужен
                // client будет собран GC
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting", e);
            }
        }
    }

    /**
     * Проверка состояния подключения
     */
    public boolean isConnected() {
        return isConnectedFlag;
    }

    /**
     * Проверка состояния подписки
     */
    public void checkConnection() {
        Log.d(TAG, "Connection state - isConnected: " + isConnected() +
                ", client state: " + (client != null ? "exists" : "null") +
                ", subscription: " + (subscription != null ? "exists" : "null"));
    }
}