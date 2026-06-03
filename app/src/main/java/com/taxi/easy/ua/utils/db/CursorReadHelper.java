package com.taxi.easy.ua.utils.db;

import android.database.Cursor;

import androidx.annotation.Nullable;

/**
 * Безопасное чтение колонок курсора ({@code getColumnIndex} может вернуть -1).
 */
public final class CursorReadHelper {

    private CursorReadHelper() {
    }

    @Nullable
    public static String getString(Cursor cursor, String columnName) {
        if (cursor == null || columnName == null) {
            return null;
        }
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getString(index) : null;
    }

    public static int getInt(Cursor cursor, String columnName) {
        if (cursor == null || columnName == null) {
            return 0;
        }
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getInt(index) : 0;
    }

    public static long getLong(Cursor cursor, String columnName) {
        if (cursor == null || columnName == null) {
            return 0L;
        }
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getLong(index) : 0L;
    }

    public static double getDouble(Cursor cursor, String columnName) {
        if (cursor == null || columnName == null) {
            return 0.0;
        }
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getDouble(index) : 0;
    }
}
