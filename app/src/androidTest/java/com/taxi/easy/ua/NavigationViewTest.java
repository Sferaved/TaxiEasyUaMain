package com.taxi.easy.ua;

import static org.junit.Assert.assertEquals;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class NavigationViewTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testNavigationToVisicomFragmentOnStartApp() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                // Сохраняем значение, которое заставляет MainActivity выполнить навигацию

                NavController navController =
                        Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);

                // Проверяем, что первый загруженный фрагмент - Visicom

                int currentDestId = Objects.requireNonNull(navController.getCurrentDestination()).getId();
                assertEquals(R.id.nav_visicom, currentDestId);
            });


        }
    }
    @Test
    public void testNavigationViewAllItems() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                // Сохраняем значение, которое заставляет MainActivity выполнить навигацию

                NavController navController =
                        Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);

                        // Массив ID пунктов меню
                        int[] menuItems = {
                                R.id.nav_visicom,
                                R.id.nav_about,
                                R.id.nav_home,
                                R.id.nav_cancel,
                                R.id.nav_settings,
                                R.id.nav_uid,
                                R.id.nav_bonus,
                                R.id.nav_card,
                                R.id.nav_account,
                                R.id.nav_author,
                                R.id.nav_finish_separate,
                                R.id.nav_restart,
                                R.id.nav_search,
                                R.id.nav_cacheOrder,
                                R.id.nav_map,
                                R.id.nav_city,
                                R.id.nav_visicom_options,
                                R.id.nav_anr
                        };

                        // Проверяем каждый пункт меню
                        for (int menuItemId : menuItems) {
                            testNavigationViewItem(menuItemId, navController);
                            int currentDestId = Objects.requireNonNull(navController.getCurrentDestination()).getId();
                            assertEquals(menuItemId, currentDestId);
                        }
            });


        }
    }

    // Универсальный метод для клика по NavigationView и проверки фрагмента
    private void testNavigationViewItem(int menuItemId, NavController navController) {
        // Кликаем по пункту меню
        navController.navigate(menuItemId, null, new NavOptions.Builder()
                .build());
     }
}
