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

import androidx.annotation.NonNull;
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
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.pusher.events.TransactionStatusEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

/**
 * Управляет подключением к Pusher и обработкой событий в реальном времени
 * Исправлена проблема с DNS, утечками памяти и дублированием событий
 */
public class PusherManager {
    private static final String PUSHER_APP_KEY = "a10fb0bff91153d35f36";
    private static final String PUSHER_CLUSTER = "mt1";
    private static final String CHANNEL_NAME = "teal-towel-48";
    private static final String TAG = "PusherManager";

    // Альтернативные хосты для резервного подключения
    private static final String[] PUSHER_HOSTS = {
            "ws-mt1.pusher.com",
            "ws.pusherapp.com",
            "ws-eu.pusher.com"
    };

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

    private static Pusher pusher = null;
    private Channel channel;
    private final WeakReference<Activity> activityRef;
    private final ExecutionStatusViewModel viewModel;
    private final Set<String> boundEvents = ConcurrentHashMap.newKeySet();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    private final PusherHandler mainHandler;

    private String lastProcessedCost = "";
    private boolean isSubscribed = false;
    private boolean isShuttingDown = false;
    private int reconnectAttempts = 0;
    private int currentHostIndex = 0;

    // Кэш DNS для устранения проблем с разрешением имен
    private static final Map<String, String> dnsCache = new ConcurrentHashMap<>();
    private boolean isSubscribing = false;
    /**
     * Безопасный Handler с WeakReference для предотвращения утечек памяти
     */
    private static class PusherHandler extends Handler {
        private final WeakReference<PusherManager> managerRef;

        PusherHandler(PusherManager manager) {
            super(Looper.getMainLooper());
            this.managerRef = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            PusherManager manager = managerRef.get();
            if (manager != null && manager.isContextValid()) {
                // Обработка сообщений если необходимо
                super.handleMessage(msg);
            }
        }

        public void postSafe(Runnable r) {
            post(() -> {
                PusherManager manager = managerRef.get();
                if (manager != null && manager.isContextValid()) {
                    r.run();
                }
            });
        }
    }

    /**
     * Интерфейс для обработки JSON событий
     */
    private interface JsonEventHandler {
        void handle(JSONObject json) throws JSONException;
    }

    public PusherManager(String eventSuffix, String userEmail, Activity context,
                         ExecutionStatusViewModel viewModel) {
        this.eventUid = "order-status-updated-" + eventSuffix + "-" + userEmail;
        this.eventUidDouble = "orderDouble-status-updated-" + eventSuffix + "-" + userEmail;
        this.eventOrder = "order-" + eventSuffix + "-" + userEmail;
        this.eventAutoOrder = "orderAuto-" + eventSuffix + "-" + userEmail;
        this.eventTransactionStatus = "transactionStatus-" + eventSuffix + "-" + userEmail;
        this.eventCanceled = "eventCanceled-" + eventSuffix + "-" + userEmail;
        this.eventBlackUserStatus = "black-user-status--" + userEmail;
        this.eventOrderCost = "order-cost-" + eventSuffix + "-" + userEmail;

        this.activityRef = new WeakReference<>(context);
        this.viewModel = viewModel;
        this.mainHandler = new PusherHandler(this);

        initializePusher();

        // Попытка установить кастомные DNS серверы
        configureDNS();
    }

    /**
     * Настройка DNS для устранения проблем с разрешением имен
     */
    private void configureDNS() {
        try {
            // Попытка установить системные DNS (может не работать на всех устройствах)
            System.setProperty("sun.net.spi.nameservice.nameservers", "8.8.8.8,8.8.4.4");
            System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom DNS", e);
        }
    }

    /**
     * Инициализация Pusher с текущим хостом
     */
    private void initializePusher() {
        PusherOptions options = new PusherOptions();
        options.setCluster(PUSHER_CLUSTER);
        options.setHost(PUSHER_HOSTS[currentHostIndex]);

        // Настройка таймаутов
        options.setActivityTimeout(60000);
        options.setPongTimeout(30000);

        pusher = new Pusher(PUSHER_APP_KEY, options);
    }

    /**
     * Переключение на альтернативный хост при проблемах с подключением
     */
    private void switchToNextHost() {
        currentHostIndex = (currentHostIndex + 1) % PUSHER_HOSTS.length;
        Log.w(TAG, "Switching to alternative host: " + PUSHER_HOSTS[currentHostIndex]);
        initializePusher();
    }

    /**
     * Разрешение DNS с кэшированием
     */
    private String resolveHostWithCache(String host) {
        if (dnsCache.containsKey(host)) {
            return dnsCache.get(host);
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            String ip = address.getHostAddress();
            dnsCache.put(host, ip);
            Log.d(TAG, "Resolved " + host + " to " + ip);
            return ip;
        } catch (UnknownHostException e) {
            Log.e(TAG, "DNS resolution failed for " + host, e);
            return host;
        }
    }

    /**
     * Проверка валидности контекста
     */
    private boolean isContextValid() {
        Activity activity = activityRef != null ? activityRef.get() : null;
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    /**
     * Получение контекста с проверкой
     */
    private Context getContext() {
        return activityRef != null ? activityRef.get() : null;
    }

    /**
     * Подключение к Pusher
     */
    public void connect() {
        if (pusher == null) {
            Log.e(TAG, "Pusher instance is not initialized!");
            return;
        }

        if (!isContextValid()) {
            Log.w(TAG, "Context is invalid, skipping connection");
            return;
        }

        resetReconnectionAttempts();

        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                logToContext("Pusher", "State changed from " + change.getPreviousState() +
                        " to " + change.getCurrentState());

                switch (change.getCurrentState()) {
                    case CONNECTED:
                        handleConnected();
                        break;
                    case DISCONNECTED:
                        handleDisconnected();
                        break;
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                handleConnectionError(message, code, e);
            }
        }, ConnectionState.ALL);
    }

    /**
     * Обработка успешного подключения
     */
    private void handleConnected() {
        logToContext("Pusher", "Successfully connected to Pusher");
        reconnectAttempts = 0;

        // Сбрасываем флаги при новом подключении
        isSubscribed = false;
        isSubscribing = false;

        if (isContextValid()) {
            subscribeToChannel();
        }
    }

    /**
     * Обработка отключения
     */
    private void handleDisconnected() {
        Log.w(TAG, "Disconnected from Pusher");
        attemptReconnection();
    }

    /**
     * Обработка ошибок подключения
     */
    private void handleConnectionError(String message, String code, Exception e) {
        String errorMsg = "Error connecting: " + message + " (Code: " + code + ")";
        logErrorToContext("Pusher", errorMsg, e);

        if (e != null) {
            FirebaseCrashlytics.getInstance().recordException(e);

            // Специфическая обработка DNS ошибки
            if (e instanceof UnknownHostException) {
                logErrorToContext("Pusher", "DNS resolution failed. Switching host...", null);
                switchToNextHost();
            }
        }

        attemptReconnection();
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

        logToContext("Pusher", "Scheduling reconnection attempt " + reconnectAttempts +
                " in " + delay + "ms");

        mainHandler.postDelayed(() -> {
            if (!isShuttingDown && isContextValid()) {
                logToContext("Pusher", "Attempting reconnection...");
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
     * Проверка, привязано ли событие
     */
    private boolean isEventBound(String eventName) {
        return boundEvents.contains(eventName);
    }

    /**
     * Безопасное привязывание события с проверкой на дублирование
     */
    private void bindEvent(String eventName, SubscriptionEventListener listener) {
        if (channel == null) {
            Log.w(TAG, "Cannot bind event " + eventName + " - channel is null");
            return;
        }

        if (!isEventBound(eventName)) {
            try {
                channel.bind(eventName, listener);
                boundEvents.add(eventName);
                Log.d(TAG, "Bound event: " + eventName);
            } catch (Exception e) {
                Log.e(TAG, "Error binding event " + eventName, e);
            }
        } else {
            Log.d(TAG, "Event already bound: " + eventName);
        }
    }

    /**
     * Общий метод для обработки JSON событий
     */
    private void handleJsonEvent(String eventName, String eventData, JsonEventHandler handler) {
        logToContext("Pusher", "Received " + eventName + ": " + eventData);

        try {
            JSONObject jsonObject = new JSONObject(eventData);

            // Создаем уникальный ключ для события
            String uniqueKey = eventName + "_" + eventData.hashCode();

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

            handler.handle(jsonObject);
        } catch (JSONException e) {
            logErrorToContext("Pusher", "JSON Parsing error for " + eventName, e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /**
     * Подписка на канал и события
     */
    public void subscribeToChannel() {
        if (!isContextValid() || pusher == null) {
            return;
        }
        // Добавить эту проверку
        if (isSubscribing) {
            Log.d(TAG, "Already subscribing, skipping");
            return;
        }

        if (isSubscribed) {
            Log.d(TAG, "Already subscribed, skipping");
            return;
        }

        isSubscribing = true; // Установить в начале
        // Проверяем, не подписаны ли мы уже на канал
        if (channel != null) {
            try {
                // Проверяем состояние канала
                Log.d(TAG, "Channel already exists, checking if subscribed: " + isSubscribed);
                if (isSubscribed) {
                    Log.d(TAG, "Already subscribed to channel, skipping");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking channel state", e);
            }
        }

        try {

            channel = pusher.subscribe(CHANNEL_NAME);
            isSubscribed = true;

            logToContext("Pusher", "Subscribing to channel: " + CHANNEL_NAME);
            logToContext("Pusher", "Subscribing to event: " + eventUid);

            // Очищаем предыдущие привязки перед новыми
            boundEvents.clear();

            bindEvent(eventUid, event -> handleUidEvent(event.getData()));
            bindEvent(eventUidDouble, event -> handleUidDoubleEvent(event.getData()));
            bindEvent(eventTransactionStatus, event -> handleTransactionStatusEvent(event.getData()));
            bindEvent(eventCanceled, event -> handleCanceledEvent(event.getData()));
            bindEvent(eventOrderCost, event -> handleOrderCostEvent(event.getData()));
            bindEvent(eventBlackUserStatus, event -> handleBlackUserStatusEvent(event.getData()));
            bindEvent(eventOrder, event -> handleOrderEvent(event.getData()));
            bindEvent(eventAutoOrder, event -> handleAutoOrderEvent(event.getData()));
            isSubscribing = false;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("Already subscribed")) {
                Log.w(TAG, "Channel already subscribed, this is expected on reconnection");
                isSubscribed = true;
                // Даже если канал уже подписан, убедимся что события привязаны
                if (channel != null && boundEvents.isEmpty()) {
                    rebindAllEvents();
                }
            } else {
                logErrorToContext("Pusher", "Error subscribing to channel", e);
                isSubscribed = false;
            }
        } catch (Exception e) {
            logErrorToContext("Pusher", "Error subscribing to channel", e);
            isSubscribed = false;
        }
    }

    /**
     * Перепривязка всех событий (для случая когда канал уже существует)
     */
    private void rebindAllEvents() {
        Log.d(TAG, "Rebinding all events to existing channel");
        boundEvents.clear();

        bindEvent(eventUid, event -> handleUidEvent(event.getData()));
        bindEvent(eventUidDouble, event -> handleUidDoubleEvent(event.getData()));
        bindEvent(eventTransactionStatus, event -> handleTransactionStatusEvent(event.getData()));
        bindEvent(eventCanceled, event -> handleCanceledEvent(event.getData()));
        bindEvent(eventOrderCost, event -> handleOrderCostEvent(event.getData()));
        bindEvent(eventBlackUserStatus, event -> handleBlackUserStatusEvent(event.getData()));
        bindEvent(eventOrder, event -> handleOrderEvent(event.getData()));
        bindEvent(eventAutoOrder, event -> handleAutoOrderEvent(event.getData()));
    }

    /**
     * Обработка UID события
     */
    private void handleUidEvent(String eventData) {
        handleJsonEvent("UID", eventData, json -> {
            String orderUid = json.getString("order_uid");
            String paySystemStatus = json.optString("paySystemStatus", "nal_payment");

            logToContext("UID", "Order UID: " + orderUid);

            mainHandler.postSafe(() -> {
                viewModel.updateUid(orderUid);
                viewModel.updatePaySystemStatus(paySystemStatus);
            });
        });
    }

    /**
     * Обработка UID Double события
     */
    private void handleUidDoubleEvent(String eventData) {
        handleJsonEvent("UID Double", eventData, json -> {
            String orderUid = json.getString("order_uid");
            String paySystemStatus = json.optString("paySystemStatus", "nal_payment");

            logToContext("Pusher Double", "Order UID Double: " + orderUid);

            mainHandler.postSafe(() -> {
                MainActivity.uid_Double = orderUid;
                MainActivity.paySystemStatus = paySystemStatus;
            });
        });
    }

    /**
     * Обработка статуса транзакции
     */



    // В методе handleTransactionStatusEvent:
    private void handleTransactionStatusEvent(String eventData) {
        handleJsonEvent("TransactionStatus", eventData, json -> {
            String uid = json.getString("uid");
            String transactionStatus = json.getString("transactionStatus");

            Log.d("Pusher", "Processing TransactionStatus - uid: " + uid + ", status: " + transactionStatus);
            Log.d("Pusher", "MainActivity.uid = " + MainActivity.uid);

            if (Objects.equals(MainActivity.uid, uid)) {
                // Отправляем событие через EventBus
                EventBus.getDefault().post(new TransactionStatusEvent(transactionStatus));

                if (isContextValid()) {
                    mainHandler.postSafe(() -> {
                        viewModel.setTransactionStatus(transactionStatus);
                        Log.d("Pusher", "Transaction status set in ViewModel: " + transactionStatus);
                    });
                }
            } else {
                Log.d("Pusher", "UID mismatch. Event uid: " + uid + ", MainActivity.uid: " + MainActivity.uid);
            }
        });
    }

    /**
     * Обработка отмены заказа
     */
    private void handleCanceledEvent(String eventData) {
        handleJsonEvent("Canceled", eventData, json -> {
            String canceled = json.getString("canceled");
            String uid = json.getString("uid");

            if (MainActivity.uid != null && MainActivity.uid.equals(uid) && isContextValid()) {
                viewModel.setCanceledStatus(canceled);
            }
        });
    }

    /**
     * Обработка стоимости заказа
     */
    private void handleOrderCostEvent(String eventData) {
        handleJsonEvent("OrderCost", eventData, json -> {
            String orderCost = json.optString("order_cost", "0");

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
        });
    }

    /**
     * Обработка статуса черного списка пользователя
     */
    private void handleBlackUserStatusEvent(String eventData) {
        handleJsonEvent("BlackUserStatus", eventData, json -> {
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
        });
    }

    /**
     * Обработка заказа
     */
    private void handleOrderEvent(String eventData) {
        handleJsonEvent("Order", eventData, json -> {
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
        });
    }

    /**
     * Обработка авто-заказа
     */
    private void handleAutoOrderEvent(String eventData) {
        handleJsonEvent("AutoOrder", eventData, json -> {
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
                    logErrorToContext("Pusher", "Error starting finish page", e);
                }
            }
        });
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
     * Отключение от Pusher
     */
    public void disconnect() {
        isShuttingDown = true;

        if (pusher != null) {
            try {
                pusher.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting", e);
            }
        }

        // Сбрасываем состояние
        isSubscribed = false;
        isSubscribing = false;
        boundEvents.clear();
        channel = null;
    }

    /**
     * Проверка состояния подключения
     */
    public boolean isConnected() {
        return pusher != null &&
                pusher.getConnection() != null &&
                pusher.getConnection().getState() == ConnectionState.CONNECTED;
    }
}