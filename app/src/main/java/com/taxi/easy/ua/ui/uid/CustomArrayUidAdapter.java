package com.taxi.easy.ua.ui.uid;


import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.NavOptions;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.db.RouteInfo;
import com.taxi.easy.ua.utils.db.RouteInfoCancel;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

public class CustomArrayUidAdapter extends ArrayAdapter<String> {

    private final String TAG = "CustomArrayCancelAdapter";
    private final int resource;
    private final int textViewId1;
    private final int textViewId2;
    private final int textViewId3;
    private final int textViewId4;
    private final int textViewId5;
    private final List<String> items;
    private RouteInfo routeInfo;
    DatabaseHelperUid databaseHelperUid;
    Context context;

    public CustomArrayUidAdapter(
            Context context,
            int resource,
            int textViewId1,
            int textViewId2,
            int textViewId3,
            int textViewId4,
            int textViewId5,
            List<String> items
    ) {
        super(context, resource, items);  // Передаем список строк в ArrayAdapter
        this.resource = resource;
        this.textViewId1 = textViewId1;
        this.textViewId2 = textViewId2;
        this.textViewId3 = textViewId3;
        this.textViewId4 = textViewId4;
        this.textViewId5 = textViewId4;
        this.items = items;
        this.context = context;
        this.databaseHelperUid = new DatabaseHelperUid(context);;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(resource, parent, false);
        }

        // Получаем TextView для отображения текста

        // Получаем строку для данного элемента
        String item = items.get(position);

        // Разделяем строку на части
        String[] parts = item.split("#"); // Разделение по запятой

        // Получаем TextViews для отображения частей
        TextView textView1 = view.findViewById(R.id.text1);
        TextView textView2 = view.findViewById(R.id.text2);
        TextView textView3 = view.findViewById(R.id.text3);
        TextView textView4 = view.findViewById(R.id.text4);
        TextView textView5 = view.findViewById(R.id.text5);
        // Добавьте дополнительные TextViews, если нужно

        // Устанавливаем текст для TextView
        if (parts.length > 0) {
            textView1.setText(parts[0]); // Первая часть
        }
        if (parts.length > 1) {
            textView2.setText(parts[1]); // Вторая часть
        }
        if (parts.length > 2) {
            textView3.setText(parts[2]); // Вторая часть
        }
        if (parts.length > 3) {
            textView4.setText(parts[3]); // Вторая часть
        }
        if (parts.length > 4) {
            textView5.setText(parts[4]); // Вторая часть
        }

        textView1.setOnClickListener(v -> {
            routeInfo = databaseHelperUid.getRouteInfoById(position+1);
            if (routeInfo != null) {
                Log.d(TAG, "onContextItemSelected: " + routeInfo);
            } else {
                Log.d(TAG, "onContextItemSelected: RouteInfo not found for id: " + (position + 1));
            }
            List<String> settings = new ArrayList<>();
            settings.add(routeInfo.getStartLat());
            settings.add(routeInfo.getStartLan());
            settings.add(routeInfo.getToLat());
            settings.add(routeInfo.getToLng());
            settings.add(routeInfo.getStart());
            settings.add(routeInfo.getFinish());

            updateRoutMarker(settings);

            MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
            SharedPreferencesHelper sharedPreferencesHelper = new SharedPreferencesHelper(context);
            sharedPreferencesHelper.saveValue("gps_upd", false);

        });
        // Получаем кнопку и устанавливаем обработчик нажатия
        AppCompatButton button = view.findViewById(R.id.button);
        button.setOnClickListener(v -> {

            routeInfo = databaseHelperUid.getRouteInfoById(position+1);
            if (routeInfo != null) {
                Log.d(TAG, "onContextItemSelected: " + routeInfo);
            } else {
                Log.d(TAG, "onContextItemSelected: RouteInfo not found for id: " + (position + 1));
            }
            List<String> settings = new ArrayList<>();
            settings.add(routeInfo.getStartLat());
            settings.add(routeInfo.getStartLan());
            settings.add(routeInfo.getToLat());
            settings.add(routeInfo.getToLng());
            settings.add(routeInfo.getStart());
            settings.add(routeInfo.getFinish());

            updateRoutMarker(settings);

            MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
            SharedPreferencesHelper sharedPreferencesHelper = new SharedPreferencesHelper(context);
            sharedPreferencesHelper.saveValue("gps_upd", false);

        });

        return view;
    }

    private void updateRoutMarker(List<String> settings) {
        Log.d(TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();

    }

}

