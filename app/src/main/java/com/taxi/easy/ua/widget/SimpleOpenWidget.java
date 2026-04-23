package com.taxi.easy.ua.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

public class SimpleOpenWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Создаем RemoteViews для виджета
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple_open);

        // Создаем Intent для открытия MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Создаем PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Устанавливаем обработчик нажатия
        views.setOnClickPendingIntent(R.id.iv_widget_icon, pendingIntent);
        views.setOnClickPendingIntent(R.id.tv_widget_text, pendingIntent);

        // Обновляем виджет
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        // Обновляем виджеты при получении любыхbroadcast
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, SimpleOpenWidget.class));
            for (int id : ids) {
                updateAppWidget(context, manager, id);
            }
        }
    }
}