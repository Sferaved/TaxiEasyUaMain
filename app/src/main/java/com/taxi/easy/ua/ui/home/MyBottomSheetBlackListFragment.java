package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import java.util.ArrayList;
import java.util.List;


public class MyBottomSheetBlackListFragment extends BottomSheetDialogFragment {
    TextView textViewCost;
    AppCompatButton btn_help;
    String cost;

    public MyBottomSheetBlackListFragment(String cost) {
        this.cost = cost;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.black_list_layout, container, false);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, getContext());
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone;
                switch (stringList.get(1)){
                    case "Kyiv City":
                        phone = "tel:0674443804";
                        break;
                    case "Dnipropetrovsk Oblast":
                        phone = "tel:0667257070";
                        break;
                    case "Odessa":
                        phone = "tel:0737257070";
                        break;
                    case "Zaporizhzhia":
                        phone = "tel:0687257070";
                        break;
                    case "Cherkasy Oblast":
                        phone = "tel:0962294243";
                        break;
                    default:
                        phone = "tel:0674443804";
                        break;
                }
                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });
        textViewCost = view.findViewById(R.id.textViewCost);
        textViewCost.setText(getString(R.string.cost_of_road) + " " + this.cost + " " +getString(R.string.UAH));
        if(this.cost.equals("orderCost")) {
            textViewCost.setVisibility(View.INVISIBLE);
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
        return list;
    }
   }

