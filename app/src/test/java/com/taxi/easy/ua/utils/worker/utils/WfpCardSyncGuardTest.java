package com.taxi.easy.ua.utils.worker.utils;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WfpCardSyncGuardTest {

    @After
    public void tearDown() {
        WfpCardSyncGuard.resetForTests();
    }

    @Test
    public void shouldApplyFetchResult_whenGenerationUnchanged_returnsTrue() {
        long gen = WfpCardSyncGuard.captureFetchGeneration();
        assertTrue(WfpCardSyncGuard.shouldApplyFetchResult(gen));
    }

    @Test
    public void shouldApplyFetchResult_afterInvalidate_returnsFalseForStaleFetch() {
        long staleGen = WfpCardSyncGuard.captureFetchGeneration();
        WfpCardSyncGuard.invalidatePendingFetches();
        assertFalse(WfpCardSyncGuard.shouldApplyFetchResult(staleGen));
    }

    @Test
    public void shouldApplyFetchResult_afterInvalidate_acceptsNewFetch() {
        WfpCardSyncGuard.invalidatePendingFetches();
        long freshGen = WfpCardSyncGuard.captureFetchGeneration();
        assertTrue(WfpCardSyncGuard.shouldApplyFetchResult(freshGen));
    }

    @Test
    public void invalidatePendingFetches_incrementsGeneration() {
        long before = WfpCardSyncGuard.captureFetchGeneration();
        long after = WfpCardSyncGuard.invalidatePendingFetches();
        assertTrue(after > before);
        assertFalse(WfpCardSyncGuard.shouldApplyFetchResult(before));
    }
}
