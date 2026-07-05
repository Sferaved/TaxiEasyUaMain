package com.taxi.easy.ua.utils.payment;

import androidx.annotation.Nullable;

/** Согласование суммы на экране заказа с ответом status API (Google Pay доплата). */
public final class FinishCostReconcileHelper {

    private FinishCostReconcileHelper() {
    }

    /**
     * Не понижать сумму на экране, пока сервер ещё не подтвердил доплату wallet-hold.
     */
    public static boolean shouldKeepDisplayedCostOverServer(
            int displayed,
            int serverTotal,
            boolean walletHold,
            boolean addCostInFlight,
            boolean addCostSheetShowing,
            @Nullable Integer walletFloorGrivna,
            boolean walletAddCostApplied
    ) {
        if (displayed <= 0 || serverTotal >= displayed) {
            return false;
        }
        if (walletAddCostApplied) {
            return true;
        }
        if (addCostInFlight || addCostSheetShowing) {
            return true;
        }
        if (!walletHold) {
            return false;
        }
        if (walletFloorGrivna == null || walletFloorGrivna <= 0) {
            return (displayed - serverTotal) <= 2;
        }
        return displayed >= walletFloorGrivna && serverTotal < walletFloorGrivna;
    }

    /** Не дублировать +N после WaitingAuthComplete, если доплата уже на экране или подтверждена. */
    public static boolean shouldSkipOptimisticWalletAdd(
            int displayedGrivna,
            @Nullable Integer walletFloorGrivna,
            boolean walletAddCostApplied
    ) {
        if (walletAddCostApplied) {
            return true;
        }
        return walletFloorGrivna != null && walletFloorGrivna > 0 && displayedGrivna >= walletFloorGrivna;
    }

    @Nullable
    public static String pickHigherCostGrivna(@Nullable String first, @Nullable String second) {
        if (first == null || first.trim().isEmpty()) {
            return second;
        }
        if (second == null || second.trim().isEmpty()) {
            return first;
        }
        try {
            int a = (int) Math.round(Double.parseDouble(first.replace(',', '.').trim()));
            int b = (int) Math.round(Double.parseDouble(second.replace(',', '.').trim()));
            return a >= b ? first.trim() : second.trim();
        } catch (NumberFormatException e) {
            return first;
        }
    }

    public static int computeOptimisticWalletTotal(
            int displayedGrivna,
            int addCostUah,
            @Nullable Integer existingFloorGrivna
    ) {
        int total = Math.max(displayedGrivna, 0) + Math.max(addCostUah, 0);
        if (existingFloorGrivna != null && total < existingFloorGrivna) {
            return existingFloorGrivna;
        }
        return total;
    }

    public static boolean serverConfirmedWalletFloor(int serverTotal, @Nullable Integer walletFloorGrivna) {
        return walletFloorGrivna != null && walletFloorGrivna > 0 && serverTotal >= walletFloorGrivna;
    }

    /** Не восстанавливать завышенную сумму после order_uid_new (12+5≠17). */
    public static int capInflatedWalletDisplay(int candidateGrivna, int authoritativeGrivna) {
        if (authoritativeGrivna <= 0) {
            return candidateGrivna;
        }
        return candidateGrivna > authoritativeGrivna ? authoritativeGrivna : candidateGrivna;
    }

    /**
     * Observer {@code finishAbsoluteCostGrivna}: wallet-hold ждёт подтверждения по uid;
     * готівка применяет итог сразу из HTTP-ответа пересоздания заказа.
     */
    public static boolean shouldApplyFinishAbsoluteCostObserver(
            boolean walletHoldPayment,
            @Nullable String currentUid,
            boolean walletAddCostAppliedForCurrentUid
    ) {
        if (!walletHoldPayment) {
            return true;
        }
        return currentUid == null || walletAddCostAppliedForCurrentUid;
    }

    /**
     * order_uid_new с order_cost: для wallet-hold это только базовый холд,
     * не финальная доплата (+5 грн на экране заказа).
     */
    public static boolean shouldTreatOrderUidNewCostAsWalletSurchargeComplete(
            boolean walletHoldPayment
    ) {
        return !walletHoldPayment;
    }

    /**
     * Пропустить checkout-доплату (+5 грн) для Google Pay / wallet-hold.
     * Начальный hold на базовую сумму не считается завершением доплаты (Mantis #21).
     */
    public static boolean shouldSkipWalletCheckoutSurchargePrompt(
            boolean walletHoldPayment,
            boolean addCostInFlight,
            boolean walletAddCostAppliedForCurrentUid,
            @Nullable Integer walletFloorGrivna,
            int displayedGrivna
    ) {
        if (!walletHoldPayment) {
            return false;
        }
        if (addCostInFlight) {
            return true;
        }
        if (walletAddCostAppliedForCurrentUid) {
            return true;
        }
        return walletFloorGrivna != null && walletFloorGrivna > 0 && displayedGrivna >= walletFloorGrivna;
    }
}
