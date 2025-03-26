package com.taxi.easy.ua.utils.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import java.util.Objects;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static long lastNavigationTime = 0;
    private static final long DEBOUNCE_DELAY = 10000; // 10 секунда задержки

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // Проверяем, прошло ли достаточно времени с последней навигации
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavigationTime < DEBOUNCE_DELAY) {
            return; // Игнорируем повторные вызовы в течение 10 секунд
        }

        // Проверяем текущий пункт назначения, чтобы избежать повторной навигации
        NavController navController = MainActivity.navController;
        int currentDestination = Objects.requireNonNull(navController.getCurrentDestination()).getId();

        if (!isConnected) {
            // Устройство не подключено к интернету
            if (currentDestination != R.id.nav_restart) {
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
                lastNavigationTime = currentTime;
            }
        }
        else {
            // Сеть восстановлена
            if (currentDestination == R.id.nav_restart) {
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
                lastNavigationTime = currentTime;
            }
        }
    }
}

