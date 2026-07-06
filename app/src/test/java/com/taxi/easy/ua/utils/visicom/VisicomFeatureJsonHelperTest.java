package com.taxi.easy.ua.utils.visicom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class VisicomFeatureJsonHelperTest {

    @Test
    public void hasRootFeatureFields_trueForSingleFeature() throws Exception {
        JSONObject json = new JSONObject(
                "{\"type\":\"Feature\",\"properties\":{\"name\":\"X\"},\"geo_centroid\":{\"type\":\"Point\",\"coordinates\":[30,50]}}");
        assertTrue(VisicomFeatureJsonHelper.hasRootFeatureFields(json));
    }

    @Test
    public void hasRootFeatureFields_falseForFeatureCollectionWithoutProperties() throws Exception {
        JSONObject json = new JSONObject("{\"type\":\"FeatureCollection\",\"features\":[]}");
        assertFalse(VisicomFeatureJsonHelper.hasRootFeatureFields(json));
        assertTrue(VisicomFeatureJsonHelper.isFeatureCollection(json));
    }

    @Test
    public void hasFeatureItemFields_falseWhenPropertiesMissing() throws Exception {
        JSONObject feature = new JSONObject("{\"type\":\"Feature\",\"geo_centroid\":{\"coordinates\":[1,2]}}");
        assertFalse(VisicomFeatureJsonHelper.hasFeatureItemFields(feature));
    }
}
