package com.taxi.easy.ua.utils.worker.utils;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class WfpUtilsResolveActiveRectokenTest {

    private Context context;
    private SharedPreferencesHelper prefs;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        prefs = new SharedPreferencesHelper(context);
        MyApplication.sharedPreferencesHelperMain = prefs;
        context.deleteDatabase(MainActivity.DB_NAME);
        context.getSharedPreferences("my_prefs", MODE_PRIVATE).edit().clear().commit();
        createTables();
        seedCity("Kyiv City");
    }

    @After
    public void tearDown() {
        context.deleteDatabase(MainActivity.DB_NAME);
        context.getSharedPreferences("my_prefs", MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void normalizeActiveFlag_mapsKnownValues() {
        assertEquals("0", WfpUtils.normalizeActiveFlag(null));
        assertEquals("1", WfpUtils.normalizeActiveFlag("1"));
        assertEquals("1", WfpUtils.normalizeActiveFlag("true"));
        assertEquals("0", WfpUtils.normalizeActiveFlag("-1"));
        assertEquals("0", WfpUtils.normalizeActiveFlag("0"));
    }

    @Test
    public void resolveActiveWfpRectoken_returnsAlreadyCheckedCard() {
        insertWfpCard("rectoken-a", "1");
        insertWfpCard("rectoken-b", "0");

        assertEquals("rectoken-a", WfpUtils.resolveActiveWfpRectoken(context));
    }

    @Test
    public void resolveActiveWfpRectoken_autoSelectsSingleUncheckedCard() {
        insertWfpCard("rectoken-only", "0");

        assertEquals("rectoken-only", WfpUtils.resolveActiveWfpRectoken(context));
        assertEquals("1", queryRectokenCheck("rectoken-only"));
    }

    @Test
    public void resolveActiveWfpRectoken_prefersCityScopedSelectionAmongMultiple() {
        insertWfpCard("rectoken-first", "0");
        insertWfpCard("rectoken-second", "0");
        WfpUtils.saveUserSelectedWfpRectoken(context, "rectoken-second");

        assertEquals("rectoken-second", WfpUtils.resolveActiveWfpRectoken(context));
        assertEquals("0", queryRectokenCheck("rectoken-first"));
        assertEquals("1", queryRectokenCheck("rectoken-second"));
    }

    @Test
    public void saveWfpCardsToDatabase_autoSelectsSingleSyncedCard() {
        CardInfo card = cardFromJson(
                "{\"rectoken\":\"393\",\"masked_card\":\"**** 4242\",\"card_type\":\"visa\","
                        + "\"bank_name\":\"Test\",\"active\":\"0\"}");

        WfpUtils.saveWfpCardsToDatabase(context, Collections.singletonList(card));

        assertEquals("393", WfpUtils.resolveActiveWfpRectoken(context));
        assertEquals("1", queryRectokenCheck("393"));
    }

    @Test
    public void saveWfpCardsToDatabase_marksPreferredRectokenOnSync() {
        WfpUtils.saveUserSelectedWfpRectoken(context, "rectoken-b");
        List<CardInfo> cards = List.of(
                cardFromJson("{\"rectoken\":\"rectoken-a\",\"masked_card\":\"**** 1111\","
                        + "\"card_type\":\"visa\",\"bank_name\":\"A\",\"active\":\"0\"}"),
                cardFromJson("{\"rectoken\":\"rectoken-b\",\"masked_card\":\"**** 2222\","
                        + "\"card_type\":\"visa\",\"bank_name\":\"B\",\"active\":\"0\"}")
        );

        WfpUtils.saveWfpCardsToDatabase(context, cards);

        assertEquals("rectoken-b", WfpUtils.resolveActiveWfpRectoken(context));
        assertEquals("1", queryRectokenCheck("rectoken-b"));
    }

    private static CardInfo cardFromJson(String json) {
        return new Gson().fromJson(json, CardInfo.class);
    }

    private void createTables() {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MainActivity.CITY_INFO
                    + "(id integer primary key autoincrement,"
                    + " city text, api text, phone text, card_max_pay text,"
                    + " bonus_max_pay text, merchant_fondy text, fondy_key_storage text)");
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MainActivity.TABLE_WFP_CARDS
                    + "(id integer primary key autoincrement,"
                    + " masked_card text, card_type text, bank_name text,"
                    + " rectoken text, merchant text, rectoken_check text)");
        } finally {
            database.close();
        }
    }

    private void seedCity(String cityName) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            ContentValues cv = new ContentValues();
            cv.put("city", cityName);
            cv.put("api", "https://test.example/");
            cv.put("phone", "+380000000000");
            database.insert(MainActivity.CITY_INFO, null, cv);
        } finally {
            database.close();
        }
    }

    private void insertWfpCard(String rectoken, String rectokenCheck) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            ContentValues cv = new ContentValues();
            cv.put("masked_card", "**** 1234");
            cv.put("card_type", "visa");
            cv.put("bank_name", "Test");
            cv.put("rectoken", rectoken);
            cv.put("merchant", "merchant");
            cv.put("rectoken_check", rectokenCheck);
            long id = database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
            assertTrue(id > 0);
        } finally {
            database.close();
        }
    }

    private String queryRectokenCheck(String rectoken) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            android.database.Cursor cursor = database.query(
                    MainActivity.TABLE_WFP_CARDS,
                    new String[]{"rectoken_check"},
                    "rectoken = ?",
                    new String[]{rectoken},
                    null, null, null);
            if (cursor.moveToFirst()) {
                String value = cursor.getString(0);
                cursor.close();
                return value;
            }
            cursor.close();
        } finally {
            database.close();
        }
        return "";
    }
}
