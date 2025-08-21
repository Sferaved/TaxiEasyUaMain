package com.taxi.easy.ua;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.utils.log.Logger;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class MainActivityUITest {

    @Mock
    private FirebaseAuth mockFirebaseAuth;

    @Mock
    private FirebaseUser mockFirebaseUser;



    @Mock
    private FirebaseAuthUIAuthenticationResult mockAuthResult;
    private AutoCloseable closeable;
    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getEmail()).thenReturn("test@example.com");


    }


    @After
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }



    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testTitleTextViewDisplaysCity() {
        Context context = ApplicationProvider.getApplicationContext();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {
                sharedPreferencesHelperMain.saveValue("CityCheckActivity", "check");
                // Вызываем onSignInResult через TestUtils
//                TestUtils.mockLoginDirect(activity, mockAuthResult);
            });
            String expectedText = context.getString(R.string.menu_city) + " " + context.getString(R.string.city_kyiv);

            // Ожидание view до 5 секунд
            onView(withId(R.id.action_bar_title)).perform(waitForView(5000));
            onView(withId(R.id.action_bar_title))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(expectedText)));
        } catch (Exception e){
            Logger.e(context, "testTitleTextViewDisplaysCity", e.toString());
        }
    }

    // Кастомное действие для ожидания
    public static ViewAction waitForView(long timeout) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Wait for view to be displayed within " + timeout + "ms";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(timeout);
            }
        };
    }
    @Test
    public void testDatabaseInitializationForUserInfoTable() {
        // Launch the MainActivity using ActivityScenario
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {
                sharedPreferencesHelperMain.saveValue("CityCheckActivity", "check");
                // Вызываем onSignInResult через TestUtils
                TestUtils.mockLoginDirect(activity, mockAuthResult);
            });
            // Get the application context
            Context context = ApplicationProvider.getApplicationContext();

            // Access the database
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);

            // Query the TABLE_USER_INFO to check if it was initialized correctly
            Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            try {
                // Verify that the table has exactly one row (as per insertUserInfo logic)
                assertEquals("Table USER_INFO should contain exactly one row after initialization", 1, cursor.getCount());

                // Move to the first row
                assertTrue("Cursor should move to first row", cursor.moveToFirst());

                // Verify the default values inserted into TABLE_USER_INFO
                assertEquals("verifyOrder should be '0'", "0", cursor.getString(cursor.getColumnIndex("verifyOrder")));
                assertEquals("phone_number should be '+38'", "+38", cursor.getString(cursor.getColumnIndex("phone_number")));
                assertEquals("email should be 'email'", "email", cursor.getString(cursor.getColumnIndex("email")));
                assertEquals("username should be 'username'", "username", cursor.getString(cursor.getColumnIndex("username")));
                assertEquals("bonus should be '0'", "0", cursor.getString(cursor.getColumnIndex("bonus")));
                assertEquals("card_pay should be '1'", "1", cursor.getString(cursor.getColumnIndex("card_pay")));
                assertEquals("bonus_pay should be '1'", "1", cursor.getString(cursor.getColumnIndex("bonus_pay")));
            } finally {
                // Close the cursor and database
                if (!cursor.isClosed()) {
                    cursor.close();
                }
                if (database.isOpen()) {
                    database.close();
                }
            }
        }
    }

}