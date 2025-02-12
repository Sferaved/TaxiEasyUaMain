package com.taxi.easy.ua.utils.pusher;

import android.util.Log;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;

import org.json.JSONException;
import org.json.JSONObject;

public class PusherManager {
    private static final String PUSHER_APP_KEY = "a10fb0bff91153d35f36"; // Ваш ключ
    private static final String PUSHER_CLUSTER = "mt1"; // Ваш кластер
    private static final String CHANNEL_NAME = "teal-towel-48"; // Канал

    private final String eventName; // Динамическое имя события
    private final Pusher pusher;
    private Channel channel;

    public PusherManager(String eventSuffix) {
        this.eventName = "order-status-updated-" + eventSuffix; // Динамическое событие

        PusherOptions options = new PusherOptions();
        options.setCluster(PUSHER_CLUSTER);

        pusher = new Pusher(PUSHER_APP_KEY, options);
    }

    // Подключение к Pusher
    public void connect() {
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Log.i("Pusher", "State changed from " + change.getPreviousState() +
                        " to " + change.getCurrentState());
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Log.e("Pusher", "Error connecting: " + message + " (Code: " + code + ")", e);
            }
        }, ConnectionState.ALL);
    }

    // Подписка на канал и событие
    public void subscribeToChannel() {
        channel = pusher.subscribe(CHANNEL_NAME);

        // Обработка события
        channel.bind(eventName, event -> {
            Log.i("Pusher", "Received event: " + event.toString());
            try {
                JSONObject eventData = new JSONObject(event.getData());
                String orderUid = eventData.getString("order_uid");

                Log.i("Pusher", "Order UID: " + orderUid);
                FinishSeparateFragment.uid = orderUid;

            } catch (JSONException e) {
                Log.e("Pusher", "JSON Parsing error", e);
            }
        });
    }

    // Отключение
    public void disconnect() {
        if (pusher != null) {
            pusher.disconnect();
        }
    }

    public String getEventName() {
        return eventName;
    }
}
