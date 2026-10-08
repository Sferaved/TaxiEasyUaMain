package com.taxi.easy.ua.utils.worker.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.city.BaseUrlHelper;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TokenUtilsBaseUrlTest {

    private SharedPreferencesHelper prefs;

    @Before
    public void setUp() {
        prefs = new SharedPreferencesHelper(RuntimeEnvironment.getApplication());
        prefs.clear();
        MyApplication.sharedPreferencesHelperMain = prefs;
    }

    @Test
    public void hasReadyBaseUrl_falseUntilDefaultsArrive() {
        assertFalse(TokenUtils.hasReadyBaseUrl());

        BaseUrlHelper.applyGlobalDefaults(prefs, "https://m.example.com", "https://t.example.com");

        assertTrue(TokenUtils.hasReadyBaseUrl());
    }

    @Test
    public void scheduleLoginReminder_whenBaseUrlMissing_doesNotThrow() {
        TokenUtils.scheduleLoginReminderIfNeeded(RuntimeEnvironment.getApplication());
    }
}
