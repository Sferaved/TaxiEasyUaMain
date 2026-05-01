package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.redmadrobot.inputmask.MaskedTextChangedListener;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.user.save_firebase.FirebaseUserManager;
import com.uxcam.UXCam;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class MyPhoneDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_PAGE = "page_arg";
    private static final String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
    private static final String TAG = "MyPhoneDialogFragment";

    private EditText phoneNumber;
    private AppCompatButton button;
    private CheckBox checkBox;
    private String page;
    private FirebaseUserManager userManager;

    // Пустой конструктор обязателен для фрагментов
    public MyPhoneDialogFragment() {
    }

    // Фабричный метод для создания экземпляра с параметрами
    public static MyPhoneDialogFragment newInstance(String page) {
        MyPhoneDialogFragment fragment = new MyPhoneDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE, page);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            page = getArguments().getString(ARG_PAGE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        UXCam.tagScreenName(TAG);

        View view = inflater.inflate(R.layout.phone_verify_layout, container, false);
        phoneNumber = view.findViewById(R.id.phoneNumber);
        button = view.findViewById(R.id.ok_button);
        checkBox = view.findViewById(R.id.checkbox);

        // Инициализация с использованием requireContext()
        phoneFull();

        button.setOnClickListener(v -> {
            String phone = formatPhoneNumber(phoneNumber.getText().toString());
            phoneNumber.setText(phone);
            if (Pattern.compile(PHONE_PATTERN).matcher(phone).matches()) {
                updateRecordsUser(phone);
                Logger.d(requireActivity(), TAG, "setOnClickListener phone: " + phone);
                Logger.d(requireActivity(), TAG, "setOnClickListener page: " + page);

                userManager = new FirebaseUserManager();
                userManager.saveUserPhone(phone);

                navigateBasedOnPage();
            } else {
                Toast.makeText(requireContext(), getString(R.string.format_phone), Toast.LENGTH_SHORT).show();
            }
        });

        setupPhoneMask();

        return view;
    }

    private void navigateBasedOnPage() {
        if (page == null) {
            dismiss();
            return;
        }

        switch (page) {
            case "home":
                HomeFragment.btnVisible(View.INVISIBLE);
                HomeFragment.btn_order.performClick();
                dismiss();
                break;
            case "visicom":
                VisicomFragment.btnStaticVisible(View.INVISIBLE);
                VisicomFragment.btnOrder.performClick();
                dismiss();
                break;
            default:
                dismiss();
                break;
        }
    }

    private void setupPhoneMask() {
        MaskedTextChangedListener listener = new MaskedTextChangedListener(
                "+38 [000] [000] [00] [00]",
                phoneNumber,
                null
        );
        phoneNumber.addTextChangedListener(listener);
        phoneNumber.setOnFocusChangeListener(listener);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBox.setOnClickListener(view -> {
            if (checkBox.isChecked()) {
                phoneNumber.setVisibility(View.VISIBLE);
                button.setVisibility(View.VISIBLE);
            } else {
                phoneNumber.setVisibility(View.INVISIBLE);
                button.setVisibility(View.INVISIBLE);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        hideKeyboard();
        if (!isPhoneValidInDatabase()) {
            restoreButtonVisibility();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(Objects.requireNonNull(requireActivity().getCurrentFocus()).getWindowToken(), 0);
        }
    }

    private void restoreButtonVisibility() {
        if (page == null) return;
        switch (page) {
            case "visicom":
                VisicomFragment.btnStaticVisible(View.VISIBLE);
                break;
            case "home":
                HomeFragment.btnVisible(View.VISIBLE);
                break;
        }
    }

    @SuppressLint("Range")
    private List<String> getUserInfoFromDatabase() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = null;
        Cursor cursor = null;

        try {
            database = requireContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    for (String columnName : cursor.getColumnNames()) {
                        list.add(cursor.getString(cursor.getColumnIndex(columnName)));
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Logger.e(requireContext(), TAG, "Error reading from database: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null) {
                database.close();
            }
        }
        return list;
    }

    private boolean isPhoneValidInDatabase() {
        List<String> userInfo = getUserInfoFromDatabase();
        if (userInfo.size() <= 2) {
            return false;
        }
        String phone = userInfo.get(2);
        return Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
    }

    @SuppressLint("Range")
    private String getPhoneFromDatabase() {
        List<String> userInfo = getUserInfoFromDatabase();
        if (userInfo.size() > 2) {
            return userInfo.get(2);
        }
        return "";
    }

    private void updateRecordsUser(String phone) {
        ContentValues cv = new ContentValues();
        cv.put("phone_number", phone);

        SQLiteDatabase database = null;
        try {
            database = requireContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            int updatedRows = database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            Logger.d(requireContext(), TAG, "Updated rows count = " + updatedRows);
            phoneNumber.setText(phone);
        } catch (Exception e) {
            Logger.e(requireContext(), TAG, "Error updating phone: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    @SuppressLint("HardwareIds")
    private void getPhoneNumberFromDevice() {
        TelephonyManager telephonyManager = (TelephonyManager) requireContext().getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Logger.d(requireContext(), TAG, "Missing permissions for reading phone number");
            return;
        }

        String phone = telephonyManager.getLine1Number();

        if (phone != null && !phone.isEmpty()) {
            String formattedPhone = formatPhoneNumber(phone);
            phoneNumber.setText(formattedPhone);
            userManager = new FirebaseUserManager();
            userManager.saveUserPhone(formattedPhone);
        } else {
            Logger.d(requireContext(), TAG, "Phone number not available, please ask user to input manually.");
        }
    }

    private void phoneFull() {
        String phone = getPhoneFromDatabase();
        phoneNumber.setText(phone);
        if (phone == null || phone.equals("+38") || phone.isEmpty()) {
            getPhoneNumberFromDevice();
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String input = phoneNumber.replaceAll("[^+\\d]", "");

        if (input.length() == 13) {
            StringBuilder formattedNumber = new StringBuilder();
            formattedNumber.append(input.substring(0, 3)).append(" ");
            formattedNumber.append(input.substring(3, 6)).append(" ");
            formattedNumber.append(input.substring(6, 9)).append(" ");
            formattedNumber.append(input.substring(9, 11)).append(" ");
            formattedNumber.append(input.substring(11, 13));
            return formattedNumber.toString();
        } else if (input.length() == 12 && !input.startsWith("+")) {
            // Если номер без +38, но с 380
            return "+" + input.substring(0, 2) + " " +
                    input.substring(2, 5) + " " +
                    input.substring(5, 8) + " " +
                    input.substring(8, 10) + " " +
                    input.substring(10, 12);
        }

        return input;
    }
}