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
            @Nullable Integer walletFloorGrivna
    ) {
        if (displayed <= 0 || serverTotal >= displayed) {
            return false;
        }
        if (addCostInFlight || addCostSheetShowing) {
            return true;
        }
        if (!walletHold || walletFloorGrivna == null || walletFloorGrivna <= 0) {
            return false;
        }
        return displayed >= walletFloorGrivna && serverTotal < walletFloorGrivna;
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
