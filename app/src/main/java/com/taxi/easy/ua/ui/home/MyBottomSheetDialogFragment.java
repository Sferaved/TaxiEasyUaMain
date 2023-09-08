package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.maps.CostJSONParser;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MyBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private String tariff;
    ListView listView;
    public String[] arrayService;
    public static String[] arrayServiceCode;
    private TextView tvSelectedTime, tvSelectedDate;
    private Calendar calendar;
    private EditText komenterinp, discount;
    Button btn_min, btn_plus;
    long discountFist;
    final static long MIN_VALUE = -90;
    final static long MAX_VALUE = 200;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_layout, container, false);
        listView = view.findViewById(R.id.list);

        arrayService = new String[]{
                getString(R.string.BAGGAGE),
                getString(R.string.ANIMAL),
                getString(R.string.CONDIT),
                getString(R.string.MEET),
                getString(R.string.COURIER),
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

        CustomListAdapter adapterSet = new CustomListAdapter(view.getContext(), arrayService, arrayService.length);
        listView.setAdapter(adapterSet);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, getContext());
        for (int i = 0; i < arrayServiceCode.length; i++) {
            if(services.get(i+1).equals("1")) {
                listView.setItemChecked(i,true);
            }
        }

        String[] tariffArr = new String[]{
                " ",
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

        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursorDb = database.query(MainActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        String tariffOld =  logCursor(MainActivity.TABLE_SETTINGS_INFO,getContext()).get(2);
        if (cursorDb != null && !cursorDb.isClosed())
            cursorDb.close();
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
                SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        tvSelectedTime = view.findViewById(R.id.tv_selected_time);


        calendar = Calendar.getInstance();
        // Добавим 10 минут к текущему времени
        calendar.add(Calendar.MINUTE, 10);
        tvSelectedTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        komenterinp = view.findViewById(R.id.komenterinp);
        discount = view.findViewById(R.id.discinp);


        discount.setText(logCursor(MainActivity.TABLE_SETTINGS_INFO, getContext()).get(3));
        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, getContext()).get(3);
        discountFist =  Integer.parseInt(discountText);


        btn_min = view.findViewById(R.id.btn_minus);
        btn_min.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discountFist -= 5;
                if (discountFist <= MIN_VALUE) {
                    discountFist = MIN_VALUE;
                }
                if(discountFist > 0) {
                    discount.setText("+" + String.valueOf(discountFist));
                } else {
                    discount.setText( String.valueOf(discountFist));
                }
            }
        });
        btn_plus = view.findViewById(R.id.btn_plus);
        btn_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discountFist += 5;
                if (discountFist >= MAX_VALUE) {
                    discountFist = MAX_VALUE;
                }
                if(discountFist > 0) {
                    discount.setText("+" + String.valueOf(discountFist));
                } else {
                    discount.setText( String.valueOf(discountFist));
                }
            }
        });

        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        tvSelectedDate.setText(currentDate.format(formatter));
        ContentValues cv = new ContentValues();
        cv.put("date", currentDate.format(formatter));

        // обновляем по id
        database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                new String[] { "1" });

        tvSelectedDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();

                // Создание диалогового окна DatePicker
                DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                        (DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) -> {
                            // Обработчик выбора даты
                            calendar.set(year, monthOfYear, dayOfMonth);
                            updateSelectedDate(calendar);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));

                // Показать диалоговое окно DatePicker
                datePickerDialog.show();
            }
        });

        database.close();



        return view;
    }
    // Метод для обновления отображаемой даты
    private void updateSelectedDate(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String formattedDate = sdf.format(calendar.getTime());
        tvSelectedDate.setText(formattedDate);
        ContentValues cv = new ContentValues();
        cv.put("date", formattedDate);

        // Обновляем по id
        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?", new String[] { "1" });
        database.close();
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPause() {
        super.onPause();
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, getContext());

        for (int i = 0; i < services.size()-1; i++) {
            ContentValues cv = new ContentValues();
            cv.put(arrayServiceCode[i], "0");
            SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
        }

        SparseBooleanArray booleanArray = listView.getCheckedItemPositions();
        for (int i = 0; i < booleanArray.size(); i++) {
            if(booleanArray.get(booleanArray.keyAt(i))) {
                ContentValues cv = new ContentValues();
                cv.put(arrayServiceCode[booleanArray.keyAt(i)], "1");
                SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();

            }
        }

        String commentText = komenterinp.getText().toString();
        if (!commentText.isEmpty()) {
            ContentValues cv = new ContentValues();

            cv.put("comment", commentText);

            // обновляем по id
            SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();
        }

        String discountText = discount.getText().toString();
        if (!discountText.isEmpty()) {

            ContentValues cv = new ContentValues();

            cv.put("discount", discountText);

            // обновляем по id
            SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();
        }
        //Проверка даты времени
        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, getContext());
        String time = stringList.get(1);
        String date = stringList.get(3);

        if(!time.equals("no_time")) {
            if(date.equals("no_date")) {
                LocalDate currentDate = LocalDate.now();

                // Получение завтрашней даты путем добавления одного дня к текущей дате
                LocalDate tomorrowDate = currentDate.plusDays(1);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                // Преобразование завтрашней даты в строку в формате "dd.MM.yyyy"
                date = tomorrowDate.format(formatter);

                ContentValues cv = new ContentValues();
                cv.put("date", date);

                // обновляем по id
                SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();

            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            // Преобразование времени и даты из строк в LocalDateTime
            LocalDateTime dateTimeFromString = LocalDateTime.parse(date + " " + time, formatter);

            LocalDateTime currentDateTimeInKyiv = LocalDateTime.now(ZoneId.of("Europe/Kiev"));

            // Сравнение дат и времени
            if (dateTimeFromString.isBefore(currentDateTimeInKyiv)) {
                Toast.makeText(getContext(), getContext().getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
                ContentValues cv = new ContentValues();

                LocalDate currentDate = LocalDate.now();

                // Получение завтрашней даты путем добавления одного дня к текущей дате
                LocalDate tomorrowDate = currentDate.plusDays(1);
                formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                // Преобразование завтрашней даты в строку в формате "dd.MM.yyyy"
                date = tomorrowDate.format(formatter);

                cv = new ContentValues();
                cv.put("date", date);

                // обновляем по id
                SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();
            }

        } else {
            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("date", "no_date");

            // обновляем по id
            SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
        }
        try {
            HomeFragment.text_view_cost.setText(changeCost());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }
    private String changeCost() throws MalformedURLException {
        String newCost = "0";
        String url = getTaxiUrlSearch(HomeFragment.from, HomeFragment.from_numberCost,
                HomeFragment.toCost, HomeFragment.to_numberCost, "costSearch", getActivity());

        Map<String, String> sendUrl = CostJSONParser.sendURL(url);

        String mes = (String) sendUrl.get("message");
        String orderC = (String) sendUrl.get("order_cost");
        Log.d("TAG", "onPausedawdddddddwdadwdawdaw orderC : " + orderC );
        if (orderC.equals("0")) {
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(mes);
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        }
        if (!orderC.equals("0")) {

            Long  firstCost = Long.parseLong(orderC);

            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, getContext()).get(3);
            long discountInt = Integer.parseInt(discountText);
            long discount;

            discount = firstCost * discountInt / 100;
            newCost = Long.toString(firstCost + discount);

            HomeFragment.cost = firstCost + discount;
            HomeFragment.addCost = discount;

        }
        return newCost;
    }
    private String getTaxiUrlSearch(String from, String from_number, String to, String to_number, String urlAPI, Context context) {

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String tarif =  logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(2);

        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        if(urlAPI.equals("costSearch")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + displayName + "(" + userEmail + ")";
        }


        // Building the url to the web service
// Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i <= 14 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d("TAG", "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        String url = "https://m.easy-order-taxi.site/" + HomeFragment.api + "/android/" + urlAPI + "/" + parameters + "/" + result;

        Log.d("TAG", "getTaxiUrlSearch: " + url);


        return url;
    }
    public static String[] arrayServiceCode() {
        return new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
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
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        String formattedTime = sdf.format(calendar.getTime());
                        tvSelectedTime.setText(formattedTime);
                        updateSelectedTime();

                            // Установленное время больше или равно текущему времени
                            tvSelectedTime.setText(formattedTime);
                            updateSelectedTime();

                            ContentValues cv = new ContentValues();
                            cv.put("time", formattedTime);

                            // Обновляем по id
                            SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?", new String[] { "1" });
                            database.close();

                    }
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void updateSelectedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(calendar.getTime());
        tvSelectedTime.setText(formattedTime);
    }
    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
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

