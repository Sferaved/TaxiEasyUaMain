package com.taxi.easy.ua.utils.city;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Регрессия: новый город без «Откуда» — карта на центре города, не на GPS из другого города.
 */
public class CityLastAddressHelperTest {

    /** Координаты из лога: авто-GPS в Киеве при выбранном Chernivtsi. */
    private static final double KIEV_GPS_LAT = 50.512825;
    private static final double KIEV_GPS_LON = 30.507257;

    @Test
    public void getCityCenter_chernivtsi_returnsKnownCenter() {
        double[] center = CityLastAddressHelper.getCityCenter("Chernivtsi");
        assertNotNull(center);
        assertArrayEquals(new double[]{48.29306, 25.93484}, center, 1e-5);
    }

    @Test
    public void getCityCenter_unknownCity_returnsNull() {
        assertNull(CityLastAddressHelper.getCityCenter("UnknownCity"));
        assertNull(CityLastAddressHelper.getCityCenter(null));
    }

    @Test
    public void isNearSelectedCity_kievGps_notNearChernivtsi() {
        assertFalse(CityLastAddressHelper.isNearSelectedCity("Chernivtsi", KIEV_GPS_LAT, KIEV_GPS_LON));
    }

    /** Первое нажатие GPS: кэш авто-GPS в Киеве не должен применяться как «рядом» с Chernivtsi. */
    @Test
    public void pendingGpsShortcut_allowedOnlyNearSelectedCity() {
        assertFalse(CityLastAddressHelper.isNearSelectedCity("Chernivtsi", KIEV_GPS_LAT, KIEV_GPS_LON));
        assertTrue(CityLastAddressHelper.isNearSelectedCity("Kyiv City", KIEV_GPS_LAT, KIEV_GPS_LON));
    }

    @Test
    public void isNearSelectedCity_chernivtsiCenter_nearChernivtsi() {
        double[] center = CityLastAddressHelper.getCityCenter("Chernivtsi");
        assertNotNull(center);
        assertTrue(CityLastAddressHelper.isNearSelectedCity("Chernivtsi", center[0], center[1]));
    }

    @Test
    public void isNearSelectedCity_kievGps_nearKyivCity() {
        assertTrue(CityLastAddressHelper.isNearSelectedCity("Kyiv City", KIEV_GPS_LAT, KIEV_GPS_LON));
    }

    @Test
    public void shouldApplyLastAddress_emptyStart_returnsFalse() {
        assertFalse(CityLastAddressHelper.shouldApplyLastAddress(
                "Chernivtsi", "48.29306", "25.93484", ""));
        assertTrue(CityLastAddressHelper.hasNoLastTrip("48.29306", ""));
    }

    @Test
    public void shouldUseGpsStartOnMap_withoutUserGpsPress_returnsFalse() {
        assertFalse(CityLastAddressHelper.shouldUseGpsStartOnMap(
                false, "вул. Тестова, місто Чернівці", 48.29, 25.93, "Chernivtsi"));
    }

    @Test
    public void shouldUseGpsStartOnMap_gpsAppliedButWrongCity_returnsFalse() {
        String kievAddress = "просп. Володимира Івасюка, місто Київ";
        assertFalse(CityLastAddressHelper.shouldUseGpsStartOnMap(
                true, kievAddress, KIEV_GPS_LAT, KIEV_GPS_LON, "Chernivtsi"));
    }

    @Test
    public void shouldUseGpsStartOnMap_gpsAppliedInSelectedCity_returnsTrue() {
        double[] center = CityLastAddressHelper.getCityCenter("Chernivtsi");
        assertNotNull(center);
        assertTrue(CityLastAddressHelper.shouldUseGpsStartOnMap(
                true, "вул. Центральна, місто Чернівці", center[0], center[1], "Chernivtsi"));
    }

    @Test
    public void shouldUseGpsStartOnMap_afterManualSelection_returnsFalse() {
        assertFalse(CityLastAddressHelper.shouldUseGpsStartOnMap(
                false, "вул. Тестова, місто Чернівці", 48.29, 25.93, "Chernivtsi"));
    }

    @Test
    public void shouldUseGpsStartOnMap_emptyAddress_returnsFalse() {
        assertFalse(CityLastAddressHelper.shouldUseGpsStartOnMap(
                true, "", KIEV_GPS_LAT, KIEV_GPS_LON, "Kyiv City"));
        assertFalse(CityLastAddressHelper.shouldUseGpsStartOnMap(
                true, "   ", KIEV_GPS_LAT, KIEV_GPS_LON, "Kyiv City"));
    }
}
