package com.taxi.easy.ua.ui.settings;

import static com.taxi.easy.ua.MainActivity.button1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentSettingsBinding;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        FragmentSettingsBinding binding = FragmentSettingsBinding.inflate(inflater, container, false);

        View root = binding.getRoot();
        if (button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }

        Spinner languageSpinner = binding.languageSpinner;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.languages,
                R.layout.custom_spinner_item
        );
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        String currentLocale = LocaleHelper.getSavedLocaleCode(requireContext());
        Logger.i(requireContext(), TAG, currentLocale);
        languageSpinner.setSelection(LocaleHelper.localeCodeToSpinnerIndex(currentLocale));

        binding.saveButton.setOnClickListener(view -> {
            int selectedIndex = languageSpinner.getSelectedItemPosition();
            String localeCode = LocaleHelper.spinnerIndexToLocaleCode(selectedIndex);
            Logger.i(requireContext(), TAG, "locale Code: " + localeCode);
            LocaleHelper.changeLanguage(requireActivity(), localeCode);
        });

        return root;
    }
}
