package com.taxi.easy.ua.utils.city;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BaseUrlHelperTest {

    private SharedPreferencesHelper prefs;

    @Before
    public void setUp() {
        prefs = new SharedPreferencesHelper(RuntimeEnvironment.getApplication());
        prefs.clear();
    }

    @Test
    public void applyGlobalDefaults_thenFallbackUsesFirestoreValues() {
        BaseUrlHelper.applyGlobalDefaults(prefs,
                "https://m.example.com/",
                "https://t.example.com/");

        assertEquals("https://t.example.com",
                BaseUrlHelper.fallbackForCity(prefs, "OdessaTest"));
        assertEquals("https://m.example.com",
                BaseUrlHelper.fallbackForCity(prefs, "Kyiv City"));
        assertEquals("https://m.example.com",
                BaseUrlHelper.fallbackForCity(prefs, null));
    }

    @Test
    public void fallbackForCity_withoutDefaults_returnsNull() {
        assertNull(BaseUrlHelper.fallbackForCity(prefs, "Kyiv City"));
        assertNull(BaseUrlHelper.fallbackForCity(prefs, "OdessaTest"));
    }

    @Test
    public void fromPrefs_usesActiveThenProd() {
        assertEquals("", BaseUrlHelper.fromPrefs(prefs));

        BaseUrlHelper.applyGlobalDefaults(prefs, "https://m.example.com", "https://t.example.com");
        assertEquals("https://m.example.com", BaseUrlHelper.fromPrefs(prefs));

        BaseUrlHelper.applyToPrefs(prefs, "https://city.example.com", "test", "Kyiv City");
        assertEquals("https://city.example.com", BaseUrlHelper.fromPrefs(prefs));
    }

    @Test
    public void normalize_trimsTrailingSlash() {
        assertEquals("https://m.example.com",
                BaseUrlHelper.normalize("https://m.example.com/"));
        assertEquals("https://m.example.com",
                BaseUrlHelper.normalize("  https://m.example.com///  "));
    }

    @Test
    public void normalize_empty_returnsNull() {
        assertNull(BaseUrlHelper.normalize(null));
        assertNull(BaseUrlHelper.normalize(""));
        assertNull(BaseUrlHelper.normalize("   "));
        assertNull(BaseUrlHelper.normalize("///"));
    }

    @Test
    public void isValidHttpUrl_acceptsHttpAndHttps() {
        assertTrue(BaseUrlHelper.isValidHttpUrl("https://m.example.com"));
        assertTrue(BaseUrlHelper.isValidHttpUrl("http://example.com/"));
        assertFalse(BaseUrlHelper.isValidHttpUrl("ftp://x"));
        assertFalse(BaseUrlHelper.isValidHttpUrl("not-a-url"));
        assertFalse(BaseUrlHelper.isValidHttpUrl(null));
    }

    @Test
    public void documentIdForCity_unknown_mapsToForeignCountries() {
        assertEquals("foreign countries", BaseUrlHelper.documentIdForCity("UnknownVille"));
        assertEquals("Kyiv City", BaseUrlHelper.documentIdForCity("Kyiv City"));
        assertEquals("OdessaTest", BaseUrlHelper.documentIdForCity("OdessaTest"));
    }

    @Test
    public void fromPrefsWithSlash_addsTrailingSlash() {
        assertEquals("", BaseUrlHelper.fromPrefsWithSlash(prefs));

        BaseUrlHelper.applyGlobalDefaults(prefs, "https://m.example.com", "https://t.example.com");
        assertEquals("https://m.example.com/", BaseUrlHelper.fromPrefsWithSlash(prefs));
    }

    @Test
    public void centrifugoWsUrl_mapsHttpsToWss() {
        BaseUrlHelper.applyGlobalDefaults(prefs, "https://m.example.com", "https://t.example.com");
        assertEquals("wss://m.example.com/connection/websocket",
                BaseUrlHelper.centrifugoWsUrl(prefs));
    }

    @Test
    public void centrifugoWsUrl_emptyWithoutDefaults() {
        assertEquals("", BaseUrlHelper.centrifugoWsUrl(prefs));
    }
}
