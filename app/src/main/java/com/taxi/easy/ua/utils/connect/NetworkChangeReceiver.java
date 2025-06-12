package com.taxi.easy.ua.utils.connect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final long DEBOUNCE_DELAY = 10000; // 10 секунд задержки
    long lastNavigationTime = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(context, "NetworkReceiver", "Received network broadcast");

        boolean isConnected = isNetworkAvailable(context);
         Logger.d(context, "NetworkReceiver", "Network status: " + (isConnected ? "Connected" : "Disconnected"));

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastNavigationTime < DEBOUNCE_DELAY) {
             Logger.d(context, "NetworkReceiver", "Ignoring due to debounce delay");
            return;
        }

        Activity activity = MyApplication.getCurrentActivity();
        if (activity == null) {
             Logger.d(context, "NetworkReceiver", "Current activity is null");
            return;
        }

        NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);
        if (navController.getCurrentDestination() == null) {
             Logger.d(context, "NetworkReceiver", "Current destination is null");
            return;
        }

        int currentDestination = navController.getCurrentDestination().getId();
        if (!isConnected) {
            if (currentDestination != R.id.nav_restart) {
                 Logger.d(context, "NetworkReceiver", "Navigating to nav_restart");
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
                lastNavigationTime= currentTime;
            }
        } else {
            if (currentDestination == R.id.nav_restart) {
                 Logger.d(context, "NetworkReceiver", "Navigating to nav_visicom");
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
                lastNavigationTime = currentTime;
            }
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
             Logger.d(context, "NetworkReceiver", "ConnectivityManager is null");
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
             Logger.d(context, "NetworkReceiver", "Active network is null");
            return false;
        }

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        boolean isConnected = networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
         Logger.d(context, "NetworkReceiver", "Network capabilities checked, connected: " + isConnected);
        return isConnected;
    }
}
