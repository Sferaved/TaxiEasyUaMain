package com.taxi.easy.ua.utils.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import java.util.Objects;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static long lastNavigationTime = 0;
    private static final long DEBOUNCE_DELAY = 10000; // 10 секунд задержки

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = isNetworkAvailable(context);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavigationTime < DEBOUNCE_DELAY) {
            return; // Игнорируем повторные вызовы в течение 10 секунд
        }

        NavController navController = MainActivity.navController;
        int currentDestination = Objects.requireNonNull(navController.getCurrentDestination()).getId();

        if (!isConnected) {
            if (currentDestination != R.id.nav_restart) {
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
                lastNavigationTime = currentTime;
            }
        } else {
            if (currentDestination == R.id.nav_restart) {
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
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
    }
}
