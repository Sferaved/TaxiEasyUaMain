package com.taxi.easy.ua.utils.city;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

public final class CityChangeRestartHelper {

    public static final String EXTRA_CITY_CHANGED = "city_changed";

    private CityChangeRestartHelper() {
    }

    public static void restartForNewCity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_CITY_CHANGED, true);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    public static void openOrderScreenIfCityChanged(MainActivity activity, NavController navController) {
        Intent intent = activity.getIntent();
        if (intent == null || !intent.getBooleanExtra(EXTRA_CITY_CHANGED, false)) {
            return;
        }
        intent.removeExtra(EXTRA_CITY_CHANGED);
        navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build());
    }
}
