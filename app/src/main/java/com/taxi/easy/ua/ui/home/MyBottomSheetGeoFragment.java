package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;

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
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MyBottomSheetGeoFragment extends BottomSheetDialogFragment {
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
    TextView texViewCost;
    private String TAG = "MyBottomSheetGeoFragment";
    TimeZone timeZone;
    SQLiteDatabase database;
    Activity context;
    public MyBottomSheetGeoFragment(TextView texViewCost) {
        this.texViewCost = texViewCost;
    }

    public MyBottomSheetGeoFragment() {
    }

     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_layout, container, false);
        listView = view.findViewById(R.id.list);
        
        context = requireActivity();
        database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

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

        CustomListAdapter adapterSet = new CustomListAdapter(context, arrayService, arrayService.length);
        listView.setAdapter(adapterSet);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        for (int i = 0; i < arrayServiceCode.length; i++) {
            if(services.get(i+1).equals("1")) {
                listView.setItemChecked(i,true);
            }
        }

        String[] tariffArr = new String[]{
                context.getResources().getString(R.string.start_t),
                context.getResources().getString(R.string.base_onl_t),
                context.getResources().getString(R.string.base_t),
                context.getResources().getString(R.string.univers_t),
                context.getResources().getString(R.string.bisnes_t),
                context.getResources().getString(R.string.prem_t),
                context.getResources().getString(R.string.econom_t),
                context.getResources().getString(R.string.bus_t),
        };
        ArrayAdapter<String> adapterTariff = new ArrayAdapter<String>(context, R.layout.my_simple_spinner_item, tariffArr);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Spinner spinner = view.findViewById(R.id.list_tariff);
        spinner.setAdapter(adapterTariff);
        spinner.setPrompt("Title");
        spinner.setBackgroundResource(R.drawable.spinner_border);

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String tariffOld =  logCursor(MainActivity.TABLE_SETTINGS_INFO,context).get(2);

        switch (tariffOld) {
            case "Базовий онлайн":
                spinner.setSelection(1);
                break;
            case  "Базовый":
                spinner.setSelection(2);
                break;
            case "Универсал":
                spinner.setSelection(3);
                break;
            case "Бизнес-класс":
                spinner.setSelection(4);
                break;
            case "Премиум-класс":
                spinner.setSelection(5);
                break;
            case "Эконом-класс":
                spinner.setSelection(6);
                break;
            case "Микроавтобус":
                spinner.setSelection(7);
                break;
            default:
                spinner.setSelection(0);;
        }



        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tariff_to_server;
                Log.d(TAG, "onItemSelected: position" + position);
                switch (position) {
                    case 0:
                        tariff_to_server = " ";
                        break;
                    case 1:
                        tariff_to_server = "Базовий онлайн";
                        break;
                    case 2:
                        tariff_to_server = "Базовый";
                        break;
                    case 3:
                        tariff_to_server = "Универсал";
                        break;
                    case 4:
                        tariff_to_server = "Бизнес-класс";
                        break;
                    case 5:
                        tariff_to_server = "Премиум-класс";
                        break;
                    case 6:
                        tariff_to_server = "Эконом-класс";
                        break;
                    case 7:
                        tariff_to_server = "Микроавтобус";
                        break;
                    default:
                        tariff_to_server = " ";
                }
                ContentValues cv = new ContentValues();
                cv.put("tarif", tariff_to_server);

                // обновляем по id
                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        timeZone = TimeZone.getDefault();
//        updateSelectedTime();
        tvSelectedTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        komenterinp = view.findViewById(R.id.komenterinp);
        discount = view.findViewById(R.id.discinp);


        discount.setText(logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3));
        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
        try {
            discountFist = Long.parseLong(discountText);
        } catch (NumberFormatException e) {
            // Handle the case where the expression cannot be evaluated
            e.printStackTrace();
        }


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
        database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                new String[] { "1" });

        tvSelectedDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // Создание диалогового окна DatePicker
                DatePickerDialog datePickerDialog = new DatePickerDialog(context,
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
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?", new String[] { "1" });
        database.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);

        for (int i = 0; i < services.size()-1; i++) {
            ContentValues cv = new ContentValues();
            cv.put(arrayServiceCode[i], "0");
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
        }

        SparseBooleanArray booleanArray = listView.getCheckedItemPositions();
        for (int i = 0; i < booleanArray.size(); i++) {
            if(booleanArray.get(booleanArray.keyAt(i))) {
                ContentValues cv = new ContentValues();
                cv.put(arrayServiceCode[booleanArray.keyAt(i)], "1");
                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();
        }

        String discountText = discount.getText().toString();
        if (!discountText.isEmpty()) {

            ContentValues cv = new ContentValues();

            cv.put("discount", discountText);

            // обновляем по id
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();
        }
        //Проверка даты времени
        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String date = stringList.get(3);
        Log.d(TAG, "onPause:time  " + time);
        Log.d(TAG, "onPause:date  " + date);
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
                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();

            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            // Преобразование времени и даты из строк в LocalDateTime
            LocalDateTime dateTimeFromString = LocalDateTime.parse(date + " " + time, formatter);

            LocalDateTime currentDateTimeInKyiv = LocalDateTime.now(ZoneId.of("Europe/Kiev"));
            Log.d(TAG, "onPause:dateTimeFromString  " + dateTimeFromString);
            Log.d(TAG, "onPause:currentDateTimeInKyiv  " + currentDateTimeInKyiv);
            // Сравнение дат и времени
            if (dateTimeFromString.isBefore(currentDateTimeInKyiv)) {
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
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
                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();
            }

        } else {
            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("date", "no_date");

            // обновляем по id
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
        }
        try {
            changeCost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }
    private void changeCost() throws MalformedURLException {

        String  url = getTaxiUrlSearchMarkers("costSearchMarkers", context);
        String message = context.getString(R.string.change_tarrif);
        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
        ToJSONParserRetrofit parser = new ToJSONParserRetrofit();

        Log.d(TAG, "orderFinished: "  + "https://m.easy-order-taxi.site"+ url);
        parser.sendURL(url, new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                Map<String, String> sendUrl = response.body();

                assert sendUrl != null;
                String orderC = sendUrl.get("order_cost");
                Log.d(TAG, "changeCost: sendUrl " + sendUrl);
                Log.d(TAG, "changeCost: orderC " + orderC);


                assert orderC != null;
                if (!orderC.equals("0")) {

                    long firstCost = Long.parseLong(orderC);


                    long discountInt = Integer.parseInt(discountText);
                    long discount = firstCost * discountInt / 100;

                    updateAddCost(String.valueOf(discount));

                    String newCost = String.valueOf(firstCost + discount);
                    if (texViewCost != null) {
                        texViewCost.setText(newCost);
                        VisicomFragment.btnVisible(View.VISIBLE);
                    }

                } else  {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    ContentValues cv = new ContentValues();
                    cv.put("tarif", " ");

                    // обновляем по id
                    SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                            new String[] { "1" });
                    database.close();
                    try {
                        changeCost();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });

    }

    private void updateRoutMarker(List<String> settings) {
        ContentValues cv = new ContentValues();

        cv.put("startLat",  Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Log.d("TAG", "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);

        // обновляем по id

        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }


    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Log.d("TAG", "getTaxiUrlSearchMarkers: " + urlAPI);

        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();

        // Получите значения полей из первой записи

        double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
        double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
        double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
        double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
        String start = cursor.getString(cursor.getColumnIndex("start"));
        String finish = cursor.getString(cursor.getColumnIndex("finish"));

        // Заменяем символ '/' в строках
        start = start.replace("/", "|");
        finish = finish.replace("/", "|");

        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

        cursor.close();


        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String comment = stringList.get(2);
        String date = stringList.get(3);



        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif =  stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        if(urlAPI.equals("costSearchMarkers")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type;
        }
        if(urlAPI.equals("orderSearchMarkersVisicom")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date+ "/" + start + "/" + finish;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });

        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i < services.size()-1 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < OpenStreetMapActivity.arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(OpenStreetMapActivity.arrayServiceCode()[i]);
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

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);

        database.close();

        return url;
    }
    private void showTimePickerDialog() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        String formattedTime = sdf.format(calendar.getTime());
                        tvSelectedTime.setText(formattedTime);


                            // Установленное время больше или равно текущему времени
                            tvSelectedTime.setText(formattedTime);
                            updateSelectedTime();

                            ContentValues cv = new ContentValues();
                            cv.put("time", formattedTime);

                            // Обновляем по id
                            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?", new String[] { "1" });
                            database.close();

                    }
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void updateSelectedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(calendar.getTime());
        Log.d(TAG, "updateSelectedTime:formattedTime " + formattedTime);
        tvSelectedTime.setText(formattedTime);
    }
    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        database.close();
        return list;
    }
}

