package com.taxi.easy.ua.utils.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashSet;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Имя вашей базы данных
    private static final String DATABASE_NAME = "Database_25022024";
    // Версия вашей базы данных
    private static final int DATABASE_VERSION = 6;
    // Имя таблицы для хранения данных routeInfo
    private static final String TABLE_ROUT_INFO = "RoutInfoTable";
    private static final String TABLE_ROUT_CANCEL = "RoutCancelTable";

    // Имена столбцов таблицы
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ROUTE_INFO = "routeInfo";
    private static final String COLUMN_UID = "uid";

    // Конструктор класса
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Создание таблицы при первом запуске приложения
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_ROUT_INFO +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_ROUTE_INFO + " TEXT)";
        db.execSQL(createTableQuery);

        createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_ROUT_CANCEL +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_UID + " TEXT," +
                COLUMN_ROUTE_INFO + " TEXT)";
        db.execSQL(createTableQuery);
    }

    // Обновление таблицы при изменении версии базы данных (если необходимо)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_INFO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_CANCEL);
        onCreate(db);
    }

    // Метод для очистки таблицы
    public void clearTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_INFO);
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_ROUT_INFO +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_ROUTE_INFO + " TEXT)";
        db.execSQL(createTableQuery);
    }
    public void clearTableCancel() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_CANCEL);
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_ROUT_CANCEL +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_UID + " TEXT," +
                COLUMN_ROUTE_INFO + " TEXT)";
        db.execSQL(createTableQuery);
    }

    // Метод для добавления данных в таблицу
    public void addRouteInfo(String routeInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ROUTE_INFO, routeInfo);
        db.insert(TABLE_ROUT_INFO, null, values);
        db.close();
    }
    public void addRouteCancel(String uid, String routeInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_UID, uid);
        values.put(COLUMN_ROUTE_INFO, routeInfo);
        db.insert(TABLE_ROUT_CANCEL, null, values);
        db.close();
    }

    public String[] readRouteInfo() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] array = null;

        // Запрос к таблице
        String query = "SELECT * FROM " + TABLE_ROUT_INFO;
        Cursor cursor = db.rawQuery(query, null);

        // Проверка наличия данных в таблице
        if (cursor != null && cursor.moveToFirst()) {
            array = new String[cursor.getCount()];

            // Индексы столбцов
            int columnIndexRouteInfo = cursor.getColumnIndex(COLUMN_ROUTE_INFO);

            // Индекс для массива
            int index = 0;

            // Чтение данных из курсора и сохранение их в массив
            do {
                String routeInfo = cleanString(cursor.getString(columnIndexRouteInfo));
                array[index] = routeInfo;
                index++;
            } while (cursor.moveToNext());

            // Закрытие курсора
            cursor.close();
        }

        // Закрытие базы данных
        db.close();

        return array;
    }
    public String[] readRouteCancel() {
        SQLiteDatabase db = this.getReadableDatabase();
        Set<String> uniqueRoutes = new HashSet<>();

        // Запрос к таблице
        String query = "SELECT * FROM " + TABLE_ROUT_CANCEL;
        Cursor cursor = db.rawQuery(query, null);

        // Проверка наличия данных в таблице
        if (cursor != null && cursor.moveToFirst()) {
            // Индексы столбцов
            int columnIndexRouteInfo = cursor.getColumnIndex(COLUMN_ROUTE_INFO);

            // Чтение данных из курсора и сохранение их в множество
            do {
                String routeInfo = cleanString(cursor.getString(columnIndexRouteInfo));
                uniqueRoutes.add(routeInfo);
            } while (cursor.moveToNext());

            // Закрытие курсора
            cursor.close();
        }

        // Закрытие базы данных
        db.close();

        // Преобразование множества в массив
        String[] array = uniqueRoutes.toArray(new String[0]);

        return array;
    }


    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ").replaceAll("\\s{2,}$", " ");
    }


}

