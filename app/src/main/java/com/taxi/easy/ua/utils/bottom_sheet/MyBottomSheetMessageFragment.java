package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
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


public class MyBottomSheetMessageFragment extends BottomSheetDialogFragment {
    TextView textViewInfo;
    AppCompatButton btn_help;
    String message;

    public MyBottomSheetMessageFragment(String message) {
        this.message = message;
    }

     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.message_list_layout, container, false);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, getContext());
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone = stringList.get(3);

                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });
        textViewInfo = view.findViewById(R.id.textViewInfo);
        textViewInfo.setText(message);
        textViewInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
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

