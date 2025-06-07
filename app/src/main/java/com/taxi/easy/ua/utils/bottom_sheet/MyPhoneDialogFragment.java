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

    public MyPhoneDialogFragment() {
    }

    EditText phoneNumber;
    AppCompatButton button;
    CheckBox checkBox;
    String page;
    final String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
    private final String TAG = "MyPhoneDialogFragment";
    private Context mContext;
    FirebaseUserManager userManager;


    public MyPhoneDialogFragment(Context context, String page) {
        this.mContext = context;
        this.page = page;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        View view = inflater.inflate(R.layout.phone_verify_layout, container, false);
        phoneNumber = view.findViewById(R.id.phoneNumber);
        button = view.findViewById(R.id.ok_button);
        checkBox = view.findViewById(R.id.checkbox);


        phoneFull(mContext);

        button.setOnClickListener(v -> {
            String phone = formatPhoneNumber(phoneNumber.getText().toString());
            phoneNumber.setText(phone);
            if (Pattern.compile(PHONE_PATTERN).matcher(phone).matches()) {

                updateRecordsUser(phoneNumber.getText().toString(), mContext);
                Logger.d(getActivity(), TAG, "setOnClickListener " + phoneNumber.getText().toString());
                Logger.d(getActivity(), TAG, "setOnClickListener " + page);

                userManager = new FirebaseUserManager();
                userManager.saveUserPhone(phoneNumber.getText().toString());

                switch (page) {
                    case "home":
                        HomeFragment.btnVisible(View.INVISIBLE);
                        HomeFragment.btn_order.performClick();
                        dismiss();
                        break;
                    case "visicom":
                        VisicomFragment.btnVisible(View.INVISIBLE);
                        VisicomFragment.btnOrder.performClick();
                        dismiss();
                        break;
                }
            } else {

                Toast.makeText(mContext, getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
            }
        });

        MaskedTextChangedListener listener = new MaskedTextChangedListener(
                "+38 [000] [000] [00] [00]",
                phoneNumber,
                null
        );

        phoneNumber.addTextChangedListener(listener);
        phoneNumber.setOnFocusChangeListener(listener);

        return view;
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = mContext.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = database.query(table, null, null, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                String str;
                do {
                    str = "";
                    for (String cn : c.getColumnNames()) {
                        str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                        list.add(c.getString(c.getColumnIndex(cn)));

                    }

                } while (c.moveToNext());
            }
        }

        return list;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(Objects.requireNonNull(requireActivity().getCurrentFocus()).getWindowToken(), 0);
        }
        if (!verifyPhone()) {
            switch (page) {
                case "visicom":
                    VisicomFragment.btnVisible(View.VISIBLE);
                    break;
                case "home":
                    HomeFragment.btnVisible(View.VISIBLE);
                    break;
            }
        }
    }

    private boolean verifyPhone() {

        List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO);

        String phone = stringList.get(2);

        Logger.d(requireActivity(), TAG, "onClick befor validate: ");
        String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(requireActivity(), TAG, "onClick No validate: " + val);
        return val;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkBox.isChecked()) {
                    phoneNumber.setVisibility(View.VISIBLE);
                    button.setVisibility(View.VISIBLE);

                } else {
                    phoneNumber.setVisibility(View.INVISIBLE);
                    button.setVisibility(View.INVISIBLE);
                }
            }
        });

    }

    private void updateRecordsUser(String result, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        int updCount = database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        Logger.d(context, TAG, "updated rows count = " + updCount);
        phoneNumber.setText(result);
    }
    @SuppressLint("HardwareIds")
    private void getPhoneNumber(Context context) {
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Logger.d(context, TAG, "Missing permissions for reading phone number");
            return;
        }

        String phone = tMgr.getLine1Number();  // старый метод, устарел, но пока работает


        if (phone != null && !phone.isEmpty()) {
            phoneNumber.setText(phone);
            userManager = new FirebaseUserManager();
            userManager.saveUserPhone(phone);
        } else {
            Logger.d(context, TAG, "Phone number not available, please ask user to input manually.");
            // Показать диалог для ручного ввода номера
        }
    }


    private void phoneFull (Context context) {
        String phone = logCursor(MainActivity.TABLE_USER_INFO).get(2);
        phoneNumber.setText(phone);
        if (phone.equals("+38") || phone.isEmpty()) {
            getPhoneNumber(context);
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        String input = phoneNumber.replaceAll("[^+\\d]", "");

        StringBuilder formattedNumber = new StringBuilder();
        if (input.length() == 13) {
            formattedNumber.append(input.substring(0, 3)).append(" ");
            formattedNumber.append(input.substring(3, 6)).append(" ");
            formattedNumber.append(input.substring(6, 9)).append(" ");
            formattedNumber.append(input.substring(9, 11)).append(" ");
            formattedNumber.append(input.substring(11, 13));
            return formattedNumber.toString();
        } else {
            return input;
        }

    }
}

