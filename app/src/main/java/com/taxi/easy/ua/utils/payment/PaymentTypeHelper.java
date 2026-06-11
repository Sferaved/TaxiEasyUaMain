package com.taxi.easy.ua.utils.payment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;

public final class PaymentTypeHelper {

    public static final String NAL = "nal_payment";
    public static final String BONUS = "bonus_payment";
    public static final String CARD = "wfp_payment";
    public static final String GOOGLE_PAY = "google_pay_payment";

    private PaymentTypeHelper() {
    }

    @NonNull
    public static String getPaymentType(@NonNull Context context) {
        return normalize(readPaymentTypeFromDb(context));
    }

    public static void setPaymentType(@NonNull Context context, @NonNull String paymentCode) {
        String normalized = normalize(paymentCode);
        ContentValues cv = new ContentValues();
        cv.put("payment_type", normalized);
        SQLiteDatabase db = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
        db.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?", new String[]{"1"});
        db.close();
        if (VisicomFragment.buttonBonus != null) {
            VisicomFragment.setBtnBonusName(context);
        }
    }

    public static void setCash(@NonNull Context context) {
        setPaymentType(context, NAL);
    }

    public static void setCard(@NonNull Context context) {
        setPaymentType(context, CARD);
    }

    public static void setBonus(@NonNull Context context) {
        setPaymentType(context, BONUS);
    }

    public static void setGooglePay(@NonNull Context context) {
        setPaymentType(context, GOOGLE_PAY);
    }

    public static boolean isCash(@Nullable String paymentType) {
        return NAL.equals(normalize(paymentType));
    }

    public static boolean isGooglePay(@Nullable String paymentType) {
        return GOOGLE_PAY.equals(normalize(paymentType));
    }

    public static boolean isCardPayment(@Nullable String paymentType) {
        String type = normalize(paymentType);
        return CARD.equals(type)
                || "card_payment".equals(type)
                || "fondy_payment".equals(type)
                || "mono_payment".equals(type);
    }

    public static boolean isBonus(@Nullable String paymentType) {
        return BONUS.equals(normalize(paymentType));
    }

    @NonNull
    private static String normalize(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return NAL;
        }
        return raw.trim();
    }

    @NonNull
    private static String readPaymentTypeFromDb(@NonNull Context context) {
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
            cursor = database.query(
                    MainActivity.TABLE_SETTINGS_INFO,
                    new String[]{"payment_type"},
                    "id = ?",
                    new String[]{"1"},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex("payment_type");
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
        return NAL;
    }
}
