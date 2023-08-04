package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.start.StartActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class MyServicesDialogFragment extends BottomSheetDialogFragment {
    private String tariff;
    ListView listView;
    public String[] arrayService;
    public static String[] arrayServiceCode;
    private TextView tvSelectedTime;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_services_layout, container, false);
        listView = view.findViewById(R.id.list);

        arrayService = new String[]{
                getString(R.string.BAGGAGE),
                getString(R.string.ANIMAL),
                getString(R.string.CONDIT),
                getString(R.string.MEET),
                getString(R.string.COURIER),
                getString(R.string.TERMINAL),
                getString(R.string.CHECK),
                getString(R.string.BABY_SEAT),
                getString(R.string.DRIVER),
                getString(R.string.NO_SMOKE),
                getString(R.string.ENGLISH),
                getString(R.string.CABLE),
                getString(R.string.FUEL),
                getString(R.string.WIRES),
                getString(R.string.SMOKE),
        };
        arrayServiceCode = new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "TERMINAL",
                "CHECK_OUT",
                "BABY_SEAT",
                "DRIVER",
                "NO_SMOKE",
                "ENGLISH",
                "CABLE",
                "FUEL",
                "WIRES",
                "SMOKE",
        };

        ArrayAdapter<String> adapterSet = new ArrayAdapter<>(view.getContext(), R.layout.services_adapter_layout, arrayService);
        listView.setAdapter(adapterSet);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        List<String> services = logCursor(StartActivity.TABLE_SERVICE_INFO, view.getContext());
        for (int i = 0; i < arrayServiceCode.length; i++) {
            if(services.get(i+1).equals("1")) {
                listView.setItemChecked(i,true);
            }
        }

        String[] tariffArr = new String[]{
                "Базовий онлайн",
                "Базовый",
                "Универсал",
                "Бизнес-класс",
                "Премиум-класс",
                "Эконом-класс",
                "Микроавтобус",
        };
        ArrayAdapter<String> adapterTariff = new ArrayAdapter<String>(view.getContext(), R.layout.my_simple_spinner_item, tariffArr);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Spinner spinner = view.findViewById(R.id.list_tariff);
        spinner.setAdapter(adapterTariff);
        spinner.setPrompt("Title");
        spinner.setBackgroundResource(R.drawable.spinner_border);

        String tariffOld =  logCursor(StartActivity.TABLE_SETTINGS_INFO,view.getContext()).get(2);
        for (int i = 0; i < tariffArr.length; i++) {
            if(tariffArr[i].equals(tariffOld)) {
                spinner.setSelection(i);
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tariff = tariffArr[position];
                ContentValues cv = new ContentValues();
                cv.put("tarif", tariff);

                // обновляем по id
                SQLiteDatabase database = view.getContext().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(StartActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        Button btnPickTime = view.findViewById(R.id.btn_pick_time);

        calendar = Calendar.getInstance();

        btnPickTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        return view;
    }
    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
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
    @Override
    public void onPause() {
        super.onPause();

        for (int i = 0; i < 15; i++) {
            SQLiteDatabase database = getContext().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
            ContentValues cv = new ContentValues();
            cv.put(arrayServiceCode[i], "0");
            database.update(StartActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
        }

        SparseBooleanArray booleanArray = listView.getCheckedItemPositions();
        booleanArray = listView.getCheckedItemPositions();
        for (int i = 0; i < booleanArray.size(); i++) {
            if(booleanArray.get(booleanArray.keyAt(i))) {
                ContentValues cv = new ContentValues();
                cv.put(arrayServiceCode[booleanArray.keyAt(i)], "1");
                SQLiteDatabase database = getContext().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(StartActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();

            }
        }

    }
    private void showTimePickerDialog() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);

                        updateSelectedTime();
                    }
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void updateSelectedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(calendar.getTime());
        tvSelectedTime.setText("Выбранное время: " + formattedTime);
    }

}

