package com.taxi.easy.ua.utils.payment;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;

/**
 * Закрытие шторки ошибки оплаты (автоотмена, отмена заказа).
 */
public final class PaymentErrorSheetHelper {

    public static final String SHEET_TAG = "payment_error_sheet";

    private PaymentErrorSheetHelper() {
    }

    public static void dismiss(@Nullable FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            return;
        }
        Fragment found = fragmentManager.findFragmentByTag(SHEET_TAG);
        if (found == null) {
            found = fragmentManager.findFragmentByTag(MyBottomSheetErrorPaymentFragment.class.getSimpleName());
        }
        if (found instanceof DialogFragment dialog && dialog.isAdded()) {
            dialog.dismissAllowingStateLoss();
        }
    }

    public static boolean isShowing(@Nullable FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            return false;
        }
        Fragment found = fragmentManager.findFragmentByTag(SHEET_TAG);
        if (found == null) {
            found = fragmentManager.findFragmentByTag(
                    MyBottomSheetErrorPaymentFragment.class.getSimpleName());
        }
        return found != null && found.isAdded();
    }
}
