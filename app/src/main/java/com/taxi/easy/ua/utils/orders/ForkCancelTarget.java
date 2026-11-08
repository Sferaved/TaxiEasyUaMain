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
}
