package com.taxi.easy.ua.utils.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Имя вашей базы данных
    private static final String DATABASE_NAME = "Database_25022024";
    // Версия вашей базы данных
    private static final int DATABASE_VERSION = 1;
    // Имя таблицы для хранения данных routeInfo
    private static final String TABLE_ROUT_INFO = "RoutInfoTable";

    // Имена столбцов таблицы
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ROUTE_INFO = "routeInfo";

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
    }

    // Обновление таблицы при изменении версии базы данных (если необходимо)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUT_INFO);
        onCreate(db);
    }

    // Метод для очистки таблицы
    public void clearTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_ROUT_INFO);
        db.close();
    }

    // Метод для добавления данных в таблицу
    public void addRouteInfo(String routeInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ROUTE_INFO, routeInfo);
        db.insert(TABLE_ROUT_INFO, null, values);
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
                String routeInfo = cursor.getString(columnIndexRouteInfo);
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
}

