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
    private static final String DATABASE_NAME = "Database_21082024_UID";
    // Версия вашей базы данных
    private static final int DATABASE_VERSION = 3;
    // Имя таблицы для хранения данных routeInfo
    private static final String TABLE_ROUT_INFO_UID = "RoutInfoTableUid";
    private static final String TABLE_CANCEL_INFO_UID = "RoutInfoTableUid";
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

        createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_CANCEL_INFO_UID + "(id integer primary key autoincrement," +
                " dispatchingOrderUid text," +
                " orderCost text," +
                " currency text," +
                " routeFrom text," +
                " routeFromNumber text," +
                " routeTo text," +
                " toNumber text," +
                " dispatchingOrderUidDouble text," +
                " pay_method text," +
                " required_time text);";
        db.execSQL(createTableQuery);
    }

    // Обновление таблицы при изменении версии базы данных (если необходимо)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_INFO_UID);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CANCEL_INFO_UID);
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
    public void clearTableCancel() {
        // Удаляем старую таблицу
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CANCEL_INFO_UID);

// Создаем новую таблицу с теми же параметрами
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_CANCEL_INFO_UID + "(id integer primary key autoincrement," +
                " dispatchingOrderUid text," +
                " orderCost text," +
                " routeFrom text," +
                " routeFromNumber text," +
                " routeTo text," +
                " toNumber text," +
                " dispatchingOrderUidDouble text,"+
                " pay_method text," +
                " required_time text," +
                " flexible_tariff_name text," +
                " comment_info text," +
                " extra_charge_codes text);";
        db.execSQL(createTableQuery);
    }

    // Метод для добавления данных в таблицу
    public void addRouteInfoUid(List<String> settings) {
        SQLiteDatabase db = this.getWritableDatabase();

        Log.d(TAG, "updateRoutMarker: " + settings.toString());
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
    public void addCancelInfoUid(List<String> settings) {
        SQLiteDatabase db = this.getWritableDatabase();

        Log.d(TAG, "addCancelInfoUid: " +settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("dispatchingOrderUid", settings.get(0));
        cv.put("orderCost", settings.get(1));
        cv.put("routeFrom", settings.get(2));
        cv.put("routeFromNumber", settings.get(3));
        cv.put("routeTo", settings.get(4));
        cv.put("toNumber", settings.get(5));
        cv.put("dispatchingOrderUidDouble", settings.get(6));
        cv.put("pay_method", settings.get(7));
        cv.put("required_time", settings.get(8));
        cv.put("flexible_tariff_name", settings.get(9));
        cv.put("comment_info", settings.get(10));
        cv.put("extra_charge_codes", settings.get(11));

        db.insert(TABLE_CANCEL_INFO_UID, null, cv);
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

    public RouteInfoCancel getCancelInfoById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Создаем запрос для получения записи по заданному id
        String query = "SELECT * FROM " + TABLE_CANCEL_INFO_UID + " WHERE id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        RouteInfoCancel routeInfo = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Предположим, что у вас есть класс RouteInfo, который хранит информацию о маршруте
                String dispatchingOrderUid = cursor.getString(cursor.getColumnIndexOrThrow("dispatchingOrderUid"));
                String orderCost = cursor.getString(cursor.getColumnIndexOrThrow("orderCost"));
                String routeFrom = cursor.getString(cursor.getColumnIndexOrThrow("routeFrom"));
                String routeFromNumber = cursor.getString(cursor.getColumnIndexOrThrow("routeFromNumber"));
                String routeTo = cursor.getString(cursor.getColumnIndexOrThrow("routeTo"));
                String toNumber = cursor.getString(cursor.getColumnIndexOrThrow("toNumber"));
                String dispatchingOrderUidDouble = cursor.getString(cursor.getColumnIndexOrThrow("dispatchingOrderUidDouble"));
                String pay_method = cursor.getString(cursor.getColumnIndexOrThrow("pay_method"));
                String required_time = cursor.getString(cursor.getColumnIndexOrThrow("required_time"));
                String flexible_tariff_name = cursor.getString(cursor.getColumnIndexOrThrow("flexible_tariff_name"));
                String comment_info = cursor.getString(cursor.getColumnIndexOrThrow("comment_info"));
                String extra_charge_codes = cursor.getString(cursor.getColumnIndexOrThrow("extra_charge_codes"));

                routeInfo = new RouteInfoCancel(
                        dispatchingOrderUid,
                        orderCost,
                        routeFrom,
                        routeFromNumber,
                        routeTo,
                        toNumber,
                        dispatchingOrderUidDouble,
                        pay_method,
                        required_time,
                        flexible_tariff_name,
                        comment_info,
                        extra_charge_codes
                        );
            }
            cursor.close();
        }

        db.close();
        return routeInfo;
    }
}
