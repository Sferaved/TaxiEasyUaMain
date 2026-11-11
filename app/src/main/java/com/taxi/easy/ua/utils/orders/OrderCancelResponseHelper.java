package com.taxi.easy.ua.utils.orders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Классификация текста ответа webordersCancel* (confirmed / pending / failure).
 */
public final class OrderCancelResponseHelper {

    public enum Kind {
        CONFIRMED,
        PENDING,
        FAILED,
        UNKNOWN
    }

    private OrderCancelResponseHelper() {
    }

    @NonNull
    public static Kind classify(@Nullable String responseText) {
        if (isFailed(responseText)) {
            return Kind.FAILED;
        }
        if (isConfirmed(responseText)) {
            return Kind.CONFIRMED;
        }
        if (isPending(responseText)) {
            return Kind.PENDING;
        }
        return Kind.UNKNOWN;
    }

    public static boolean isFailed(@Nullable String responseText) {
        if (responseText == null || responseText.isEmpty()) {
            return true;
        }
        String lower = responseText.toLowerCase(Locale.ROOT);
        return lower.contains("не вдалося")
                || lower.contains("не вдалось")
                || lower.contains("did not cancel");
    }

    public static boolean isConfirmed(@Nullable String responseText) {
        if (isFailed(responseText)) {
            return false;
        }
        if (responseText == null || responseText.isEmpty()) {
            return false;
        }
        String lower = responseText.toLowerCase(Locale.ROOT);
        return lower.contains("скасоване") || lower.contains("скасовано");
    }

    public static boolean isPending(@Nullable String responseText) {
        if (isFailed(responseText) || isConfirmed(responseText)) {
            return false;
        }
        if (responseText == null || responseText.isEmpty()) {
            return false;
        }
        String lower = responseText.toLowerCase(Locale.ROOT);
        return lower.contains("очікуємо")
                || (lower.contains("надіслано") && !lower.contains("скасоване"));
    }

    /**
     * Принимать push eventCanceled: да, если пользователь уже ждёт отмену;
     * иначе — только когда опрос не показывает активный поиск.
     */
    public static boolean shouldAcceptServerCanceledPush(
            boolean orderAlreadyCanceled,
            boolean cancelAwaitingConfirmation,
            boolean orderCanceledByPoll,
            int lastCloseReason
    ) {
        if (orderAlreadyCanceled) {
            return false;
        }
        if (cancelAwaitingConfirmation) {
            return orderCanceledByPoll
                    || (lastCloseReason >= 1 && lastCloseReason != 8);
        }
        if (orderCanceledByPoll) {
            return true;
        }
        return lastCloseReason != -1 && lastCloseReason != 0;
    }

    /**
     * Пока ждём оператора, push «canceled» не закрывает экран сам:
     * опрос ещё может видеть поиск. Нужно сразу перечитать статус.
     */
    public static boolean shouldRefreshStatusAfterCanceledPush(
            boolean orderAlreadyCanceled,
            boolean operatorConfirmationPending,
            boolean pushAccepted
    ) {
        return !orderAlreadyCanceled && operatorConfirmationPending && !pushAccepted;
    }

    /**
     * Возврат на тот же заказ не должен сбрасывать ожидание подтверждения отмены.
     * Иначе экран остаётся на «очікуємо диспетчера», а опрос статуса больше не идёт.
     */
    public static boolean shouldKeepCancelWatchOnSameOrder(
            boolean cancelInFlight,
            boolean awaitingOperator
    ) {
        return cancelInFlight || awaitingOperator;
    }

    /**
     * После фона нужно снова спросить статус, пока отмена не подтверждена на экране.
     */
    public static boolean shouldResumeCancelStatusWatch(
            boolean cancelUiAlreadyShown,
            boolean awaitingOperator,
            boolean cancelWatchStillArmed
    ) {
        if (cancelUiAlreadyShown) {
            return false;
        }
        return awaitingOperator || cancelWatchStillArmed;
    }
}
