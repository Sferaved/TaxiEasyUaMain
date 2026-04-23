package com.taxi.easy.ua.utils.connect;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.log.Logger;

public class NetworkMonitor {
    private static final long DEBOUNCE_DELAY_MS = 2000; // 2 seconds debounce
    private static final String TAG = "NetworkMonitor";

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isRegistered = false;
    private long lastNavigationTime = 0;
    private boolean wasConnected = false; // Отслеживаем предыдущее состояние

    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Logger.d(context, TAG, "NetworkMonitor initialized");
    }

    public void startMonitoring(Activity activity) {
        if (connectivityManager == null) {
            Logger.e(context, TAG, "ConnectivityManager is null, cannot start monitoring");
            return;
        }

        if (isRegistered) {
            Logger.d(context, TAG, "NetworkCallback already registered, skipping");
            return;
        }

        // Инициализируем начальное состояние
        wasConnected = isInternetAvailable(connectivityManager);
        Logger.d(context, TAG, "Initial internet state: " + wasConnected);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Logger.d(context, TAG, "onAvailable called, network: " + network);
                // Не делаем ничего, ждем onCapabilitiesChanged
            }

            @Override
            public void onLost(@NonNull Network network) {
                Logger.w(context, TAG, "onLost called, network: " + network);
                handleNetworkChange(false, activity, "onLost");
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                Logger.d(context, TAG, "onCapabilitiesChanged called for network: " + network);

                boolean hasInternet = isInternetAvailable(networkCapabilities);
                boolean hasValidatedCapability = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                boolean hasInternetCapability = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

                Logger.d(context, TAG, String.format(
                        "Network capabilities - Has Internet: %b, Has VALIDATED: %b, Has INTERNET: %b, Transport types: %s",
                        hasInternet, hasValidatedCapability, hasInternetCapability, getTransportTypes(networkCapabilities)
                ));

                // Проверяем, изменилось ли состояние интернета
                if (hasInternet != wasConnected) {
                    Logger.i(context, TAG, "Internet state changed: " + wasConnected + " -> " + hasInternet);
                    wasConnected = hasInternet;
                    handleNetworkChange(hasInternet, activity, "capabilities_changed");
                } else {
                    Logger.d(context, TAG, "Internet state unchanged: " + hasInternet + ", skipping navigation");
                }
            }

            @Override
            public void onUnavailable() {
                Logger.w(context, TAG, "onUnavailable called - no networks available");
                handleNetworkChange(false, activity, "onUnavailable");
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        isRegistered = true;
        Logger.i(context, TAG, "Network monitoring started successfully");
    }

    public void stopMonitoring() {
        if (!isRegistered || connectivityManager == null || networkCallback == null) {
            Logger.d(context, TAG, "Cannot stop monitoring: isRegistered=" + isRegistered +
                    ", connectivityManager=" + connectivityManager +
                    ", networkCallback=" + networkCallback);
            return;
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            isRegistered = false;
            wasConnected = false;
            Logger.i(context, TAG, "Network monitoring stopped successfully");
        } catch (IllegalArgumentException e) {
            Logger.e(context, TAG, "Failed to unregister NetworkCallback: " + e.getMessage());
        } finally {
            networkCallback = null;
        }
    }

    private void handleNetworkChange(boolean isConnected, Activity activity, String trigger) {
        Logger.d(context, TAG, String.format("handleNetworkChange called - isConnected: %b, trigger: %s", isConnected, trigger));

        // Проверка валидности Activity
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Logger.e(context, TAG, String.format("Activity invalid - isNull: %b, isFinishing: %b, isDestroyed: %b",
                    activity == null, activity != null && activity.isFinishing(), activity != null && activity.isDestroyed()));
            return;
        }

        // Проверка наличия NavHostFragment
        View navHostView = activity.findViewById(R.id.nav_host_fragment_content_main);
        if (navHostView == null) {
            Logger.e(context, TAG, "nav_host_fragment_content_main not found in Activity: " + activity.getClass().getSimpleName());
            return;
        }

        // Получение NavController
        NavController navController;
        try {
            navController = Navigation.findNavController(navHostView);
            Logger.d(context, TAG, "NavController obtained successfully");
        } catch (Exception e) {
            Logger.e(context, TAG, "Failed to get NavController: " + e.getMessage());
            return;
        }

        // Проверка текущего destination
        if (navController.getCurrentDestination() == null) {
            Logger.e(context, TAG, "Current destination is null, cannot perform navigation");
            return;
        }

        int currentDestinationId = navController.getCurrentDestination().getId();
        String currentDestinationName = getDestinationName(currentDestinationId);
        Logger.d(context, TAG, String.format("Current destination - ID: %d, Name: %s", currentDestinationId, currentDestinationName));

        // Debounce check
        long currentTime = System.currentTimeMillis();
        long timeSinceLastNavigation = currentTime - lastNavigationTime;

        if (timeSinceLastNavigation < DEBOUNCE_DELAY_MS) {
            Logger.d(context, TAG, String.format("Ignoring network change due to debounce delay - time since last nav: %d ms (limit: %d ms)",
                    timeSinceLastNavigation, DEBOUNCE_DELAY_MS));
            return;
        }

        // Логика навигации
        if (!isConnected && currentDestinationId != R.id.nav_restart) {
            Logger.w(context, TAG, "NO INTERNET - Navigating to nav_restart from " + currentDestinationName);
            performNavigation(navController, R.id.nav_restart, currentTime);
        }
        else if (isConnected && currentDestinationId == R.id.nav_restart) {
            Logger.i(context, TAG, "INTERNET RESTORED - Navigating to nav_visicom from restart screen");
            performNavigation(navController, R.id.nav_visicom, currentTime);
        }
        else if (isConnected && currentDestinationId != R.id.nav_restart) {
            Logger.d(context, TAG, "Internet connected and not on restart screen - no navigation needed");
        }
        else if (!isConnected && currentDestinationId == R.id.nav_restart) {
            Logger.d(context, TAG, "No internet but already on restart screen - no navigation needed");
        }
    }

    private void performNavigation(NavController navController, int destinationId, long currentTime) {
        mainHandler.post(() -> {
            try {
                String destName = getDestinationName(destinationId);
                Logger.i(context, TAG, "Performing navigation to: " + destName);

                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(destinationId, true)
                        .setLaunchSingleTop(true)
                        .build();

                navController.navigate(destinationId, null, navOptions);
                lastNavigationTime = currentTime;
                Logger.i(context, TAG, "Navigation to " + destName + " completed successfully");
            } catch (Exception e) {
                Logger.e(context, TAG, "Navigation failed: " + e.getMessage());
            }
        });
    }

    private boolean isInternetAvailable(NetworkCapabilities capabilities) {
        if (capabilities == null) {
            Logger.d(context, TAG, "isInternetAvailable: capabilities is null");
            return false;
        }

        boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        Logger.d(context, TAG, "isInternetAvailable: " + hasInternet);
        return hasInternet;
    }

    private boolean isInternetAvailable(ConnectivityManager cm) {
        if (cm == null) {
            Logger.e(context, TAG, "isInternetAvailable: ConnectivityManager is null");
            return false;
        }

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            Logger.d(context, TAG, "isInternetAvailable: No active network");
            return false;
        }

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return isInternetAvailable(capabilities);
    }

    private String getTransportTypes(NetworkCapabilities capabilities) {
        if (capabilities == null) return "none";

        StringBuilder transports = new StringBuilder();
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) transports.append("WiFi,");
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) transports.append("Cellular,");
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) transports.append("Ethernet,");
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) transports.append("VPN,");
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) transports.append("Bluetooth,");

        String result = transports.length() > 0 ? transports.toString() : "none";
        return result.substring(0, result.length() - 1);
    }

    private String getDestinationName(int destinationId) {
        if (destinationId == R.id.nav_restart) return "nav_restart";
        if (destinationId == R.id.nav_visicom) return "nav_visicom";
        if (destinationId == R.id.nav_home) return "nav_home";
        return "unknown_" + destinationId;
    }
}