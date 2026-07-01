package com.taxi.easy.ua.utils.payment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.taxi.easy.ua.R;

/**
 * Человекочитаемые подписи способа оплаты для списков заказов.
 */
public final class PaymentMethodDisplayHelper {

    private PaymentMethodDisplayHelper() {
    }

    public static boolean hasCarInfo(@Nullable String auto) {
        if (auto == null) {
            return false;
        }
        String trimmed = auto.trim();
        return !trimmed.isEmpty() && !"??".equals(trimmed);
    }

    @NonNull
    public static String formatAutoLine(@NonNull Context context, @Nullable String auto) {
        if (!hasCarInfo(auto)) {
            return "";
        }
        return context.getString(R.string.auto_info) + " " + auto.trim();
    }

    @NonNull
    public static String formatPaymentLine(@NonNull Context context, @Nullable String payMethod) {
        if (payMethod == null || payMethod.trim().isEmpty()) {
            return "";
        }
        return context.getString(R.string.payment_type_select)
                + ": "
                + resolveMethodLabel(context, payMethod.trim());
    }

    @NonNull
    public static String resolveMethodLabel(@NonNull Context context, @NonNull String payMethod) {
        if (PaymentTypeHelper.BONUS.equals(payMethod)) {
            return context.getString(R.string.pay_method_message_bonus);
        }
        if (PaymentTypeHelper.GOOGLE_PAY.equals(payMethod)) {
            return context.getString(R.string.pay_method_message_google);
        }
        if (PaymentTypeHelper.CARD.equals(payMethod)
                || "card_payment".equals(payMethod)
                || "fondy_payment".equals(payMethod)
                || "mono_payment".equals(payMethod)) {
            return context.getString(R.string.pay_method_message_card);
        }
        if (PaymentTypeHelper.NAL.equals(payMethod)) {
            return context.getString(R.string.pay_method_message_nal);
        }
        return context.getString(R.string.pay_method_message_nal);
    }
}
