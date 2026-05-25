package com.taxi.easy.ua.utils.payment;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;
import com.taxi.easy.ua.utils.log.Logger;

/**
 * Закрытие шторки ошибки оплаты (автоотмена, отмена заказа).
 */
public final class PaymentErrorSheetHelper {

    private static final String TAG = "PaymentErrorSheetHelper";

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
            Logger.d(MyApplication.getContext(), TAG,
                    "[cashReorder] dismiss payment sheet tag="
                            + (found.getTag() != null ? found.getTag() : "?"));
            dialog.dismissAllowingStateLoss();
        } else {
            Logger.d(MyApplication.getContext(), TAG, "[cashReorder] dismiss: sheet not found");
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
        boolean showing = found != null && found.isAdded();
        if (showing) {
            Logger.d(MyApplication.getContext(), TAG,
                    "[cashReorder] isShowing=true tag=" + (found != null ? found.getTag() : "?"));
        }
        return showing;
    }
}
