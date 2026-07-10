package com.taxi.easy.ua.ui.landing;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;

/**
 * Смена языка со стартового экрана без перехода в настройки.
 */
public final class LandingLanguageHelper {

    private static final int[] OPTION_ROOT_IDS = {
            R.id.langOptionEn,
            R.id.langOptionRu,
            R.id.langOptionUk
    };

    private static final int[] OPTION_LABEL_IDS = {
            R.string.landing_language_label_en,
            R.string.landing_language_label_ru,
            R.string.landing_language_label_uk
    };

    private static final String[] OPTION_BADGES = {"EN", "RU", "UA"};

    private LandingLanguageHelper() {
    }

    public static void showLanguagePicker(@NonNull FragmentActivity activity) {
        String currentLocale = LocaleHelper.getSavedLocaleCode(activity);
        int checkedIndex = LocaleHelper.localeCodeToSpinnerIndex(currentLocale);

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_landing_language, null);
        final int[] selectedIndex = {checkedIndex};

        bindLanguageOptions(activity, dialogView, selectedIndex);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.langDialogBtnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.langDialogBtnSave).setOnClickListener(v -> {
            String localeCode = LocaleHelper.spinnerIndexToLocaleCode(selectedIndex[0]);
            if (!localeCode.equals(LocaleHelper.normalizeLocaleCode(currentLocale))) {
                LocaleHelper.changeLanguage(activity, localeCode);
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private static void bindLanguageOptions(@NonNull FragmentActivity activity,
                                            @NonNull View dialogView,
                                            @NonNull int[] selectedIndex) {
        for (int i = 0; i < OPTION_ROOT_IDS.length; i++) {
            View optionRoot = dialogView.findViewById(OPTION_ROOT_IDS[i]);
            TextView badge = optionRoot.findViewById(R.id.langOptionBadge);
            TextView label = optionRoot.findViewById(R.id.langOptionLabel);
            badge.setText(OPTION_BADGES[i]);
            label.setText(OPTION_LABEL_IDS[i]);

            final int index = i;
            optionRoot.setOnClickListener(v -> {
                selectedIndex[0] = index;
                refreshLanguageSelection(dialogView, selectedIndex[0]);
            });
        }
        refreshLanguageSelection(dialogView, selectedIndex[0]);
    }

    private static void refreshLanguageSelection(@NonNull View dialogView, int selectedIndex) {
        for (int i = 0; i < OPTION_ROOT_IDS.length; i++) {
            View optionRoot = dialogView.findViewById(OPTION_ROOT_IDS[i]);
            ImageView check = optionRoot.findViewById(R.id.langOptionCheck);
            boolean selected = i == selectedIndex;
            optionRoot.setBackgroundResource(selected
                    ? R.drawable.bg_landing_language_option_selected
                    : R.drawable.bg_landing_language_option);
            check.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }
}
