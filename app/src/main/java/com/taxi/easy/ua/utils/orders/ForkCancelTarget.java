package com.taxi.easy.ua.utils.orders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Второй uid вилки. Пустая строка, пробел и «*» означают, что пары нет
 * и отменять нужно обе части через webordersCancelDouble, а не одиночный заказ.
 */
public final class ForkCancelTarget {

    private ForkCancelTarget() {
    }

    @NonNull
    public static String normalizeDoubleUid(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "*".equals(trimmed) || "null".equals(trimmed)) {
            return "";
        }
        return trimmed;
    }

    /**
     * Живое поле могло остаться пустым после раннего перехода на экран заказа.
     * Тогда берём uid, который уже записан из ответа сервера.
     */
    @NonNull
    public static String resolveDoubleUid(@Nullable String live, @Nullable String persisted) {
        String fromLive = normalizeDoubleUid(live);
        if (!fromLive.isEmpty()) {
            return fromLive;
        }
        return normalizeDoubleUid(persisted);
    }

    public static boolean hasLinkedDouble(@Nullable String live, @Nullable String persisted) {
        return !resolveDoubleUid(live, persisted).isEmpty();
    }

    /**
     * Ответ статуса вилки может нести номер наличной половины.
     * Это та же пара, а не пересозданный заказ.
     */
    public static boolean isForkPartnerStatusUid(
            @Nullable String activeUid,
            @Nullable String partnerUid,
            @Nullable String responseUid
    ) {
        String partner = normalizeDoubleUid(partnerUid);
        String response = normalizeDoubleUid(responseUid);
        String active = normalizeDoubleUid(activeUid);
        return !partner.isEmpty()
                && partner.equals(response)
                && !partner.equals(active);
    }

    /**
     * Номер, который можно записать как активный заказ.
     * Канонический uid из поля uid важнее номера ноги в dispatching_order_uid.
     */
    @NonNull
    public static String statusUidToAdopt(
            @Nullable String activeUid,
            @Nullable String partnerUid,
            @Nullable String dispatchUid,
            @Nullable String canonicalUid
    ) {
        String active = normalizeDoubleUid(activeUid);
        String partner = normalizeDoubleUid(partnerUid);
        String dispatch = normalizeDoubleUid(dispatchUid);
        String canonical = normalizeDoubleUid(canonicalUid);
        if (!canonical.isEmpty() && canonical.equals(active)) {
            return active;
        }
        if (!dispatch.isEmpty() && !partner.isEmpty() && dispatch.equals(partner)) {
            return active;
        }
        if (!canonical.isEmpty() && !canonical.equals(partner)) {
            return canonical;
        }
        if (!dispatch.isEmpty() && !dispatch.equals(partner)) {
            return dispatch;
        }
        return active;
    }
}
