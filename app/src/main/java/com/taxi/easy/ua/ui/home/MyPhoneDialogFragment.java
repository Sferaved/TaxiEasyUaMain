package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.start.StartActivity;

import java.util.regex.Pattern;


public class MyPhoneDialogFragment extends BottomSheetDialogFragment {
    EditText phoneNumber;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phone_verify_layout, container, false);
        phoneNumber = view.findViewById(R.id.phoneNumber);
        Button button = view.findViewById(R.id.ok_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();

                if (!val) {
                    Toast.makeText(getActivity(), getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
                }
                if (val) {
                    StartActivity.verifyPhone = true;
                    updateRecordsUser(phoneNumber.getText().toString(), getContext());
                    dismiss();
                }
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

    }
    public static void updateRecordsUser(String result, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        int updCount = database.update(StartActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        Log.d("TAG", "updated rows count = " + updCount);


    }

}

