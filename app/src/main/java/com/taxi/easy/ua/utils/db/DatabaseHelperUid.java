package com.taxi.easy.ua.utils.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

public class DatabaseHelperUid extends SQLiteOpenHelper {
    // Имя вашей базы данных
    private static final String DATABASE_NAME = "Database_21052024_UID";
    // Версия вашей базы данных
    private static final int DATABASE_VERSION = 1;
    // Имя таблицы для хранения данных routeInfo
    private static final String TABLE_ROUT_INFO_UID = "RoutInfoTableUid";
    private final String TAG ="DatabaseHelperUid";

    // Конструктор класса
    public DatabaseHelperUid(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Создание таблицы при первом запуске приложения
    @Override
    public void onCreate(SQLiteDatabase db) {

        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_ROUT_INFO_UID + "(id integer primary key autoincrement," +
                " startLat text," +
                " startLan text," +
                " to_lat text," +
                " to_lng text," +
                " start text," +
                " finish text);";
        db.execSQL(createTableQuery);
    }

    // Обновление таблицы при изменении версии базы данных (если необходимо)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_INFO_UID);
        onCreate(db);
    }

    // Метод для очистки таблицы
    public void clearTableUid() {
        // Удаляем старую таблицу
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_INFO_UID);

// Создаем новую таблицу с теми же параметрами
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_ROUT_INFO_UID + "(id integer primary key autoincrement," +
                " startLat text," +
                " startLan text," +
                " to_lat text," +
                " to_lng text," +
                " start text," +
                " finish text);";
        db.execSQL(createTableQuery);

    }

    // Метод для добавления данных в таблицу
    public void addRouteInfoUid(List<String> settings) {
        SQLiteDatabase db = this.getWritableDatabase();

        Log.d(TAG, "updateRoutMarker: " + settings.toString());
        Log.d(TAG, "updateRoutMarker: " + settings.get(0));
        Log.d(TAG, "updateRoutMarker: " + settings.get(1));
        Log.d(TAG, "updateRoutMarker: " + settings.get(2));
        Log.d(TAG, "updateRoutMarker: " + settings.get(3));
        Log.d(TAG, "updateRoutMarker: " + settings.get(4));
        Log.d(TAG, "updateRoutMarker: " + settings.get(5));
        ContentValues cv = new ContentValues();

        cv.put("startLat", settings.get(0));
        cv.put("startLan", settings.get(1));
        cv.put("to_lat", settings.get(2));
        cv.put("to_lng", settings.get(3));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));

        db.insert(TABLE_ROUT_INFO_UID, null, cv);
        db.close();
    }
    public RouteInfo getRouteInfoById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Создаем запрос для получения записи по заданному id
        String query = "SELECT * FROM " + TABLE_ROUT_INFO_UID + " WHERE id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        RouteInfo routeInfo = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Предположим, что у вас есть класс RouteInfo, который хранит информацию о маршруте
                String startLat = cursor.getString(cursor.getColumnIndexOrThrow("startLat"));
                String startLan = cursor.getString(cursor.getColumnIndexOrThrow("startLan"));
                String toLat = cursor.getString(cursor.getColumnIndexOrThrow("to_lat"));
                String toLng = cursor.getString(cursor.getColumnIndexOrThrow("to_lng"));
                String start = cursor.getString(cursor.getColumnIndexOrThrow("start"));
                String finish = cursor.getString(cursor.getColumnIndexOrThrow("finish"));

                routeInfo = new RouteInfo(startLat, startLan, toLat, toLng, start, finish);
            }
            cursor.close();
        }

        db.close();
        return routeInfo;
    }
}
