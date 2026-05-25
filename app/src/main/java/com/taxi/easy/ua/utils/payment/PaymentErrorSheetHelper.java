package com.taxi.easy.ua.utils.payment;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Закрытие шторки ошибки оплаты (автоотмена, отмена заказа).
 */
public final class PaymentErrorSheetHelper {

    private static final String TAG = "PaymentErrorSheetHelper";

    public static final String SHEET_TAG = "payment_error_sheet";

    private static final AtomicBoolean SHOW_IN_FLIGHT = new AtomicBoolean(false);

    private PaymentErrorSheetHelper() {
    }

    /** Один показ шторки за раз (гонка observer + refresh + view.post). */
    public static boolean beginShowAttempt() {
        boolean acquired = SHOW_IN_FLIGHT.compareAndSet(false, true);
        if (!acquired) {
            Logger.d(MyApplication.getContext(), TAG, "beginShowAttempt: already in flight");
        }
        return acquired;
    }

    public static void releaseShowLock() {
        SHOW_IN_FLIGHT.set(false);
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
        releaseShowLock();
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
