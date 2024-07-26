package com.taxi.easy.ua.ui.home;

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
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;

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
    final String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
    private final String TAG = "MyPhoneDialogFragment";
    private Context mContext;
    public MyPhoneDialogFragment(Context context, String page) {
        this.mContext = context;
        this.page = page;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phone_verify_layout, container, false);
        phoneNumber = view.findViewById(R.id.phoneNumber);
        button = view.findViewById(R.id.ok_button);
        checkBox = view.findViewById(R.id.checkbox);

        MainActivity.verifyPhone = false;
        phoneFull(mContext);

        button.setOnClickListener(v -> {
            if (Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches()) {
                MainActivity.verifyPhone = true;
                updateRecordsUser(phoneNumber.getText().toString(), mContext);
                Logger.d(getActivity(), TAG, "setOnClickListener " + phoneNumber.getText().toString());
                Logger.d(getActivity(), TAG, "setOnClickListener " + page);
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
                MainActivity.verifyPhone = false;
                Toast.makeText(mContext, getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
            }
        });


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
        if (!MainActivity.verifyPhone) {
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
    private void getPhoneNumber (Context context) {
        String phone;
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Logger.d(context, TAG, "Manifest.permission.READ_PHONE_NUMBERS: " + ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS));
            Logger.d(context, TAG, "Manifest.permission.READ_PHONE_STATE: " + ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE));
            return;
        }
        phone = tMgr.getLine1Number();

        if(phone != null) {
            phoneNumber.setText(phone);
        }

    }
    private void phoneFull (Context context) {
        String phone = logCursor(MainActivity.TABLE_USER_INFO).get(2);
        phoneNumber.setText(phone);
        if (phone.equals("+380") || phone.isEmpty()) {
            getPhoneNumber(context);
        }
    }
}

