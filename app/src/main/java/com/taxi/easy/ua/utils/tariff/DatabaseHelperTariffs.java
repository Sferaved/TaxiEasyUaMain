package com.taxi.easy.ua.utils.tariff;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class DatabaseHelperTariffs extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tariffs.db";
    private static final int DATABASE_VERSION = 1;

    // Названия таблицы и столбцов
    public static final String TABLE_TARIFFS = "tariffs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_FLEXIBLE_TARIFF_NAME = "flexible_tariff_name";
    public static final String COLUMN_DISPATCHING_ORDER_UID = "dispatching_order_uid";
    public static final String COLUMN_ORDER_COST = "order_cost";
    public static final String COLUMN_ADD_COST = "add_cost";
    public static final String COLUMN_RECOMMENDED_ADD_COST = "recommended_add_cost";
    public static final String COLUMN_CURRENCY = "currency";
    public static final String COLUMN_DISCOUNT_TRIP = "discount_trip";
    public static final String COLUMN_CAN_PAY_BONUSES = "can_pay_bonuses";
    public static final String COLUMN_CAN_PAY_CASHLESS = "can_pay_cashless";

    // Создание таблицы
    private static final String CREATE_TABLE_TARIFFS = "CREATE TABLE IF NOT EXISTS " + TABLE_TARIFFS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_FLEXIBLE_TARIFF_NAME + " TEXT," +
            COLUMN_DISPATCHING_ORDER_UID + " TEXT," +
            COLUMN_ORDER_COST + " TEXT," +
            COLUMN_ADD_COST + " TEXT," +
            COLUMN_RECOMMENDED_ADD_COST + " TEXT," +
            COLUMN_CURRENCY + " TEXT," +
            COLUMN_DISCOUNT_TRIP + " TEXT," +
            COLUMN_CAN_PAY_BONUSES + " TEXT," +
            COLUMN_CAN_PAY_CASHLESS + " TEXT" +
            ")";

    public DatabaseHelperTariffs(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TARIFFS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TARIFFS);
        onCreate(db);
    }
    public void insertOrUpdateTariff(String flexibleTariffName,
                                     String dispatchingOrderUid,
                                     String orderCost,
                                     String addCost,
                                     String recommendedAddCost,
                                     String currency,
                                     String discountTrip,
                                     String canPayBonuses,
                                     String canPayCashless) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Проверяем, существует ли уже запись с таким flexibleTariffName
        String selection = COLUMN_FLEXIBLE_TARIFF_NAME + "=?";
        String[] selectionArgs = {flexibleTariffName};
        Cursor cursor = db.query(TABLE_TARIFFS, null, selection, selectionArgs, null, null, null);
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) {
            cursor.close();
        }

        // Создаем объект ContentValues для вставки или обновления данных
        ContentValues values = new ContentValues();
        values.put(COLUMN_FLEXIBLE_TARIFF_NAME, flexibleTariffName);
        values.put(COLUMN_DISPATCHING_ORDER_UID, dispatchingOrderUid);
        values.put(COLUMN_ORDER_COST, orderCost);
        values.put(COLUMN_ADD_COST, addCost);
        values.put(COLUMN_RECOMMENDED_ADD_COST, recommendedAddCost);
        values.put(COLUMN_CURRENCY, currency);
        values.put(COLUMN_DISCOUNT_TRIP, discountTrip);
        values.put(COLUMN_CAN_PAY_BONUSES, canPayBonuses);
        values.put(COLUMN_CAN_PAY_CASHLESS, canPayCashless);

        // Вставляем или обновляем данные в зависимости от наличия записи с flexibleTariffName
        if (exists) {
            // Если запись существует, обновляем ее
            db.update(TABLE_TARIFFS, values, selection, selectionArgs);
        } else {
            // Если запись не существует, вставляем новую
            db.insert(TABLE_TARIFFS, null, values);
        }

//        db.close();
    }


    public List<String> getTariffDetailsByFlexibleTariffName(String flexibleTariffNameSearch, List<String> tariffDetailsList) {

        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_DISPATCHING_ORDER_UID, COLUMN_ORDER_COST,
                COLUMN_ADD_COST, COLUMN_RECOMMENDED_ADD_COST, COLUMN_CURRENCY, COLUMN_DISCOUNT_TRIP,
                COLUMN_CAN_PAY_BONUSES, COLUMN_CAN_PAY_CASHLESS};
        String selection = COLUMN_FLEXIBLE_TARIFF_NAME + "=?";
        String[] selectionArgs = {flexibleTariffNameSearch};
        Cursor cursor = db.query(TABLE_TARIFFS, columns, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String dispatchingOrderUid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISPATCHING_ORDER_UID));
                String orderCost = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ORDER_COST));
                String addCost = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADD_COST));
                String recommendedAddCost = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECOMMENDED_ADD_COST));
                String currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY));
                String discountTrip = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISCOUNT_TRIP));
                String canPayBonuses = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAN_PAY_BONUSES));
                String canPayCashless = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAN_PAY_CASHLESS));
                // Формируем строку с данными о тарифе

                tariffDetailsList.add(flexibleTariffNameSearch);
                tariffDetailsList.add(dispatchingOrderUid);
                tariffDetailsList.add(orderCost);
                tariffDetailsList.add(addCost);
                tariffDetailsList.add(recommendedAddCost);
                tariffDetailsList.add(currency);
                tariffDetailsList.add(discountTrip);
                tariffDetailsList.add(canPayBonuses);
                tariffDetailsList.add(canPayCashless);

            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return tariffDetailsList;
    }


}
