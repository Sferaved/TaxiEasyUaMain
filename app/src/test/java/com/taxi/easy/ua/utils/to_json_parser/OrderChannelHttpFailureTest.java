package com.taxi.easy.ua.utils.to_json_parser;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrderChannelHttpFailureTest {

    @Test
    public void canceledAfterUid_doesNotShowToast() {
        assertFalse(OrderChannelHttpFailure.shouldShowNetworkErrorToast(true, true));
        assertFalse(OrderChannelHttpFailure.shouldShowNetworkErrorToast(true, false));
        assertFalse(OrderChannelHttpFailure.shouldShowNetworkErrorToast(false, true));
    }

    @Test
    public void realNetworkFailure_showsToast() {
        assertTrue(OrderChannelHttpFailure.shouldShowNetworkErrorToast(false, false));
    }
}
