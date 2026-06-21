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
        if (!walletHold || walletFloorGrivna == null || walletFloorGrivna <= 0) {
            return false;
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
}
