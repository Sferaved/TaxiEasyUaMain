package com.taxi.easy.ua.utils.payment;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.taxi.easy.ua.utils.cost.CostParseHelper;

/**
 * WFP add-cost HTTP body already includes new uid/cost after recreate —
 * do not rely on Centrifugo alone (Mantis #31).
 */
public final class WalletAddCostHttpRecreateHelper {

    private WalletAddCostHttpRecreateHelper() {
    }

    public static boolean hasRecreatedOrder(@Nullable String uid) {
        return uid != null && !uid.trim().isEmpty();
    }

    @Nullable
    public static String resolveDisplayCostGrivna(
            @Nullable JsonElement clientCost,
            @Nullable JsonElement webCost
    ) {
        String fromClient = costFromJsonElement(clientCost);
        if (fromClient != null) {
            return fromClient;
        }
        return costFromJsonElement(webCost);
    }

    @Nullable
    public static String resolveDisplayCostGrivna(
            @Nullable Object clientCost,
            @Nullable Object webCost
    ) {
        String fromClient = costFromObject(clientCost);
        if (fromClient != null) {
            return fromClient;
        }
        return costFromObject(webCost);
    }

    /** Apply immediately when CHARGE response includes uid; else wait for order_uid_new. */
    public static boolean shouldApplyHttpRecreateOnHoldSuccess(
            @Nullable String transactionStatus,
            @Nullable String uid
    ) {
        if (!hasRecreatedOrder(uid)) {
            return false;
        }
        if (transactionStatus == null) {
            return false;
        }
        return "Approved".equals(transactionStatus)
                || "WaitingAuthComplete".equals(transactionStatus);
    }

    @Nullable
    private static String costFromJsonElement(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return CostParseHelper.normalizeCostString(element.getAsString());
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static String costFromObject(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonElement) {
            return costFromJsonElement((JsonElement) value);
        }
        return CostParseHelper.normalizeCostString(String.valueOf(value));
    }
}
