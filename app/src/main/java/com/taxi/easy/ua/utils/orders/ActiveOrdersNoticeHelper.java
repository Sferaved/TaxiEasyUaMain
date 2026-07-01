package com.taxi.easy.ua.utils.orders;

/**
 * Решает, показывать ли шторку «Замовлення в роботі» на странице заказа ({@code VisicomFragment}).
 */
public final class ActiveOrdersNoticeHelper {

    private ActiveOrdersNoticeHelper() {
    }

    /**
     * @param currentNavDestinationId {@link com.taxi.easy.ua.MainActivity#currentNavDestination}
     *        или id из {@link androidx.navigation.NavController#getCurrentDestination()}
     */
    public static boolean shouldOfferOnOrderPage(
            boolean suppressAfterCancel,
            int currentNavDestinationId,
            int navVisicomId,
            int navFinishSeparateId,
            int navCacheOrderId,
            boolean submitInProgress,
            boolean earlyNavigationDone,
            boolean googlePaySubmitFrozen,
            boolean googlePayProcessingUiShown,
            boolean hasPersistedActiveOrderUid
    ) {
        if (suppressAfterCancel) {
            return false;
        }
        if (hasPersistedActiveOrderUid) {
            return false;
        }
        if (currentNavDestinationId == navFinishSeparateId
                || currentNavDestinationId == navCacheOrderId) {
            return false;
        }
        if (currentNavDestinationId != navVisicomId) {
            return false;
        }
        if (submitInProgress || earlyNavigationDone) {
            return false;
        }
        if (googlePaySubmitFrozen || googlePayProcessingUiShown) {
            return false;
        }
        return true;
    }
}
