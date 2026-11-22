package com.taxi.easy.ua.utils.bugreport;

import androidx.annotation.Nullable;

/**
 * Picks a usable host for the bug-report dialog.
 * Prefer the click/view context (usually the live Activity) over a stale global pointer.
 */
public final class BugReportHostResolver {

    private BugReportHostResolver() {
    }

    @Nullable
    public static <T> T resolve(@Nullable Object primary,
                                @Nullable Object secondary,
                                Class<T> type) {
        if (type == null) {
            return null;
        }
        if (type.isInstance(primary)) {
            return type.cast(primary);
        }
        if (type.isInstance(secondary)) {
            return type.cast(secondary);
        }
        return null;
    }
}
