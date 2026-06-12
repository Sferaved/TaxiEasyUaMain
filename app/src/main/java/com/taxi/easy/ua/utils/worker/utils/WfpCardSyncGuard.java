package com.taxi.easy.ua.utils.worker.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Защита от гонки: устаревший ответ синхронизации карт не перезаписывает БД после удаления.
 */
public final class WfpCardSyncGuard {

    private static final AtomicLong SYNC_GENERATION = new AtomicLong(0);

    private WfpCardSyncGuard() {
    }

    public static long captureFetchGeneration() {
        return SYNC_GENERATION.get();
    }

    public static long invalidatePendingFetches() {
        return SYNC_GENERATION.incrementAndGet();
    }

    public static boolean shouldApplyFetchResult(long fetchGeneration) {
        return fetchGeneration == SYNC_GENERATION.get();
    }

    static void resetForTests() {
        SYNC_GENERATION.set(0);
    }
}
