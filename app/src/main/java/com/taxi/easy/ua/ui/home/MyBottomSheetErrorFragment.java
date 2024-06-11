package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import java.util.ArrayList;
import java.util.List;


public class MyBottomSheetErrorFragment extends BottomSheetDialogFragment {
    TextView textViewInfo;
    AppCompatButton btn_help, btn_ok;
    String errorMessage;

    public MyBottomSheetErrorFragment(String errorMessage) {
        this.errorMessage = errorMessage;
    }

     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_list_layout, container, false);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone = stringList.get(3);

                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });

        btn_ok = view.findViewById(R.id.btn_ok);

        textViewInfo = view.findViewById(R.id.textViewInfo);
        Log.d("TAG", "onCreateView:errorMessage " + errorMessage);
        if (errorMessage != null && !errorMessage.equals("null")) {
            textViewInfo.setText(errorMessage);
            if (errorMessage.equals(getString(R.string.verify_internet))
                || errorMessage.equals(getString(R.string.error_message))
            ) {
                btn_ok.setVisibility(View.GONE);
                textViewInfo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        ContentValues cv = new ContentValues();

                        cv.put("email", "email");

                        // обновляем по id
                        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                                new String[] { "1" });
                        database.close();
                        dismiss();
                        startActivity(new Intent(requireContext(), MainActivity.class));
                    }
                });
            } else if(errorMessage.equals(getString(R.string.server_error_connected))) {
                textViewInfo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
                btn_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
            } else {
                btn_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
            }
        } else {
            textViewInfo.setText(getString(R.string.error_message));
            btn_ok.setText(getString(R.string.try_again));
            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(requireContext(), MainActivity.class));
                }
            });
        }

        return view;
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
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
        database.close();
        assert c != null;
        c.close();
        return list;
    }
}

