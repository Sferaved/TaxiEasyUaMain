package com.taxi.easy.ua.utils.worker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.inclusive.InclusiveTransportPromptCoordinator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class InclusiveTransportPreferenceWorkerTest {

    private static final String EMAIL = "client@example.com";

    @Before
    public void setUp() {
        MyApplication.sharedPreferencesHelperMain =
                new com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper(RuntimeEnvironment.getApplication());
    }

    @Test
    public void hasBeenAskedForCurrentUser_usesPerEmailKey() {
        MyApplication.sharedPreferencesHelperMain.saveValue("userEmail", EMAIL);
        assertFalse(InclusiveTransportPreferenceWorker.hasBeenAskedForCurrentUser());

        MyApplication.sharedPreferencesHelperMain.saveValue(
                InclusiveTransportPreferenceWorker.askedKey(EMAIL), true);
        assertTrue(InclusiveTransportPreferenceWorker.hasBeenAskedForCurrentUser());
    }

    @Test
    public void saveUserPreference_storesEnabledPerEmail() {
        MyApplication.sharedPreferencesHelperMain.saveValue("userEmail", EMAIL);
        InclusiveTransportPreferenceWorker.saveUserPreference(true);

        assertTrue(InclusiveTransportPreferenceWorker.needsInclusiveTransport());
        assertTrue(InclusiveTransportPreferenceWorker.hasBeenAskedForCurrentUser());
    }

    @Test
    public void onAuthSucceeded_doesNotThrow() {
        InclusiveTransportPromptCoordinator.onAuthSucceeded();
    }
}
