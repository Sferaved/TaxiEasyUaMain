package com.taxi.easy.ua.utils.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.utils.log.Logger;

public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private static final long STABILIZATION_DELAY_MS = 1500;

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isRegistered = false;
    private boolean isCurrentlyConnected = true;
    private NetworkChangeListener listener;
    private Runnable pendingNetworkAction = null;

    public interface NetworkChangeListener {
        void onNetworkChanged(boolean isConnected);
    }

    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.isCurrentlyConnected = checkInternetSync();
    }

    public void setListener(NetworkChangeListener listener) {
        this.listener = listener;
    }

    public void startMonitoring() {
        if (connectivityManager == null || isRegistered) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                scheduleNetworkCheck();
            }

            @Override
            public void onLost(@NonNull Network network) {
                scheduleNetworkCheck();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                scheduleNetworkCheck();
            }

            @Override
            public void onUnavailable() {
                scheduleNetworkCheck();
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        isRegistered = true;
        Logger.i(context, TAG, "Network monitoring started");

        notifyListeners(isCurrentlyConnected);
    }

    public void stopMonitoring() {
        if (!isRegistered || connectivityManager == null || networkCallback == null) return;

        if (pendingNetworkAction != null) {
            mainHandler.removeCallbacks(pendingNetworkAction);
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            isRegistered = false;
            Logger.i(context, TAG, "Network monitoring stopped");
        } catch (IllegalArgumentException e) {
            Logger.e(context, TAG, "Error: " + e.getMessage());
        }
    }

    private void scheduleNetworkCheck() {
        if (pendingNetworkAction != null) {
            mainHandler.removeCallbacks(pendingNetworkAction);
        }

        pendingNetworkAction = () -> {
            boolean newState = checkInternetSync();
            if (newState != isCurrentlyConnected) {
                isCurrentlyConnected = newState;
                Logger.i(context, TAG, "Network state changed: " + (newState ? "connected" : "disconnected"));
                notifyListeners(newState);
            }
            pendingNetworkAction = null;
        };

        mainHandler.postDelayed(pendingNetworkAction, STABILIZATION_DELAY_MS);
    }

    private void notifyListeners(boolean isConnected) {
        if (listener != null) {
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onNetworkChanged(isConnected);
                }
            });
        }
    }

    private boolean checkInternetSync() {
        if (connectivityManager == null) return false;

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return false;

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public boolean isConnected() {
        return isCurrentlyConnected;
    }

    /**
     * Принудительно проверяет интернет и уведомляет слушателя
     * Полезно вызывать при нажатии кнопки "Повторить"
     */
    public void forceCheck() {
        mainHandler.post(() -> {
            boolean newState = checkInternetSync();
            if (newState != isCurrentlyConnected) {
                isCurrentlyConnected = newState;
                Logger.i(context, TAG, "Force check - state changed: " + (newState ? "connected" : "disconnected"));
                notifyListeners(newState);
            } else {
                Logger.d(context, TAG, "Force check - state unchanged: " + (newState ? "connected" : "disconnected"));
                // Всё равно уведомляем, чтобы обновить UI
                notifyListeners(newState);
            }
        });
    }

    /**
     * Возвращает детальную информацию о сети (для отладки)
     */
    public String getNetworkInfo() {
        if (connectivityManager == null) return "ConnectivityManager is null";

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return "No active network";

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return "No network capabilities";

        boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        boolean isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        String transport = isWifi ? "WiFi" : (isCellular ? "Cellular" : "Other");

        return String.format("%s - Internet: %b, Validated: %b", transport, hasInternet, isValidated);
    }
}