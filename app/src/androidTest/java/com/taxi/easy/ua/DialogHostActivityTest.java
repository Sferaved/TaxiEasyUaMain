package com.taxi.easy.ua;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.taxi.easy.ua.utils.worker.utils.DialogHostActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DialogHostActivityTest {

    @Test
    public void testDialogHostActivityLaunchesAndClosesWithBottomSheet() throws Exception {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                DialogHostActivity.class
        );

        try (ActivityScenario<DialogHostActivity> scenario = ActivityScenario.launch(intent)) {

            // Проверяем, что BottomSheet появился
            String textNotifyMessage =
                    InstrumentationRegistry.getInstrumentation()
                            .getTargetContext()
                            .getString(R.string.app_name) + ": " +
                            InstrumentationRegistry.getInstrumentation()
                                    .getTargetContext()
                                    .getString(R.string.sentNotifyMessage);

            onView(withText(textNotifyMessage)).check(matches(isDisplayed()));

            // Закрываем BottomSheet
            scenario.onActivity(activity -> {

                if (DialogHostActivity.bottomSheetDialogFragment != null) {
                    DialogHostActivity.bottomSheetDialogFragment.dismiss();
                }
            });

            // Ждём завершения активити
            int retries = 0;
            while (scenario.getState() != Lifecycle.State.DESTROYED && retries < 10) {
                Thread.sleep(300);
                retries++;
            }

            // Проверяем, что активити действительно закрылась
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }
}
