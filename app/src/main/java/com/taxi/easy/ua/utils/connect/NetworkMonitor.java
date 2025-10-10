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
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;

public class NetworkMonitor {
    private static final long DEBOUNCE_DELAY_MS = 1000; // 10 seconds debounce delay
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isRegistered = false;
    private long lastNavigationTime = 0;

    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void startMonitoring(Activity activity) {
        if (connectivityManager == null) {
            Logger.e(context, "NetworkMonitor", "ConnectivityManager is null");
            return;
        }

        if (isRegistered) {
            Logger.d(context, "NetworkMonitor", "NetworkCallback already registered");
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Logger.d(context, "NetworkMonitor", "Network available");
                handleNetworkChange(true, MyApplication.getCurrentActivity());
            }

            @Override
            public void onLost(@NonNull Network network) {
                Logger.d(context, "NetworkMonitor", "Network lost");
                handleNetworkChange(false, activity);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                boolean isConnected = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                Logger.d(context, "NetworkMonitor", "Network capabilities changed, connected: " + isConnected);
                handleNetworkChange(isConnected, activity);
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        isRegistered = true;
        Logger.d(context, "NetworkMonitor", "Network monitoring started");
    }

    public void stopMonitoring() {
        if (!isRegistered || connectivityManager == null || networkCallback == null) {
            Logger.d(context, "NetworkMonitor", "Cannot stop monitoring: not registered or null components");
            return;
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            isRegistered = false;
            Logger.d(context, "NetworkMonitor", "Network monitoring stopped");
        } catch (IllegalArgumentException e) {
            Logger.e(context, "NetworkMonitor", "Failed to unregister NetworkCallback: " + e.getMessage());
        } finally {
            networkCallback = null;
        }
    }

    private void handleNetworkChange(boolean isConnected, Activity activity) {
        if (activity == null || activity.isFinishing()) {
            Logger.e(context, "NetworkMonitor", "Activity is null or finishing, skipping navigation");
            return;
        }
        if (activity.findViewById(R.id.nav_host_fragment_content_main) == null) {
            Logger.e(context, "NetworkMonitor", "NavHostFragment view not found in activity");
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavigationTime < DEBOUNCE_DELAY_MS) {
            Logger.d(context, "NetworkMonitor", "Ignoring network change due to debounce delay");
            return;
        }
        mainHandler.post(() -> {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                Logger.e(context, "NetworkMonitor", "Activity is not valid for navigation");
                return;
            }

            View navHostView = activity.findViewById(R.id.nav_host_fragment_content_main);
            if (navHostView == null) {
                Logger.e(context, "NetworkMonitor", "nav_host_fragment_content_main not found in this Activity: " + activity.getClass().getSimpleName());
                return;
            }

            NavController navController;
            try {
                navController = Navigation.findNavController(navHostView);
            } catch (Exception e) {
                Logger.e(context, "NetworkMonitor", "Failed to get NavController: " + e.getMessage());
                return;
            }

            if (navController.getCurrentDestination() == null) {
                Logger.e(context, "NetworkMonitor", "Current destination is null");
                return;
            }

            int currentDestination = navController.getCurrentDestination().getId();
            if (!isConnected && currentDestination != R.id.nav_restart) {
                Logger.d(context, "NetworkMonitor", "Navigating to nav_restart");
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
                lastNavigationTime = currentTime;
            } else if (isConnected && currentDestination == R.id.nav_restart) {
                Logger.d(context, "NetworkMonitor", "Navigating to nav_visicom");
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
                lastNavigationTime = currentTime;
            }
        });

    }
}