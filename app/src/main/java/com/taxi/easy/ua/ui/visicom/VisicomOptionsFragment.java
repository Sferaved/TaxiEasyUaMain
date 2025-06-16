package com.taxi.easy.ua.ui.visicom;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.CustomListAdapter;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.log.Logger;
import com.uxcam.UXCam;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class VisicomOptionsFragment extends Fragment {
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

    private final String TAG = "VisicomOptionsFragment";
    TimeZone timeZone;
    SQLiteDatabase database;
    Activity context;


     
    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        View view = inflater.inflate(R.layout.fragment_visicom_options, container, false);
        listView = view.findViewById(R.id.list);
        
        context = requireActivity();
        database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        sharedPreferencesHelperMain.saveValue("time", "no_time");
        sharedPreferencesHelperMain.saveValue("date", "no_date");
//        ContentValues cv = new ContentValues();
//        cv.put("time", "no_time");
//        cv.put("date", "no_date");
//        database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
//                new String[] { "1" });

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
        arrayServiceCode = DataArr.arrayServiceCode();

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
                spinner.setSelection(0);
        }

        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
        String city = stringList.get(1);
        String cityMenu;
//        switch (city){
//            case "Kyiv City":
//                cityMenu = getString(R.string.city_kyiv);
//                break;
//            case "Dnipropetrovsk Oblast":
//                cityMenu = getString(R.string.city_dnipro);
//                break;
//            case "Odessa":
//                cityMenu = getString(R.string.city_odessa);
//                break;
//            case "Zaporizhzhia":
//                cityMenu = getString(R.string.city_zaporizhzhia);
//                break;
//            case "Cherkasy Oblast":
//                cityMenu = getString(R.string.city_cherkassy);
//                break;
//            case "Lviv":
//                cityMenu = getString(R.string.city_lviv);
//                break;
//            case "Ivano_frankivsk":
//                cityMenu = getString(R.string.city_ivano_frankivsk);
//                break;
//            case "Vinnytsia":
//                cityMenu = getString(R.string.city_vinnytsia);
//                break;
//            case "Poltava":
//                cityMenu = getString(R.string.city_poltava);
//                break;
//            case "Sumy":
//                cityMenu = getString(R.string.city_sumy);
//                break;
//            case "Kharkiv":
//                cityMenu = getString(R.string.city_kharkiv);
//                break;
//            case "Chernihiv":
//                cityMenu = getString(R.string.city_chernihiv);
//                break;
//            case "Rivne":
//                cityMenu = getString(R.string.city_rivne);
//                break;
//            case "Ternopil":
//                cityMenu = getString(R.string.city_ternopil);
//                break;
//            case "Khmelnytskyi":
//                cityMenu = getString(R.string.city_khmelnytskyi);
//                break;
//            case "Zakarpattya":
//                cityMenu = getString(R.string.city_zakarpattya);
//                break;
//            case "Zhytomyr":
//                cityMenu = getString(R.string.city_zhytomyr);
//                break;
//            case "Kropyvnytskyi":
//                cityMenu = getString(R.string.city_kropyvnytskyi);
//                break;
//            case "Mykolaiv":
//                cityMenu = getString(R.string.city_mykolaiv);
//                break;
//            case "Chernivtsi":
//                cityMenu = getString(R.string.city_chernivtsi);
//                break;
//            case "Lutsk":
//                cityMenu = getString(R.string.city_lutsk);
//                break;
//            case "OdessaTest":
//                cityMenu = "Test";
//                break;
//            default:
//                cityMenu = getString(R.string.foreign_countries);
//        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tariff_to_server;
                Logger.d(context, TAG, "onItemSelected: position" + position);
                switch (position) {
                    case 0:
                        tariff_to_server = " ";
                        break;
                    case 1:
                        if(city.equals("Kyiv City")) {
                            tariff_to_server = "Базовий онлайн";
                        } else {
                            tariff_to_server = " ";
                        }
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
                sharedPreferencesHelperMain.saveValue("tarif", tariff_to_server);

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
        updateSelectedTime();
        tvSelectedTime.setOnClickListener(v -> showTimePickerDialog());

        komenterinp = view.findViewById(R.id.komenterinp);
        discount = view.findViewById(R.id.discinp);

        String comment = (String) sharedPreferencesHelperMain.getValue("comment", "no_comment");

        if (!comment.equals("no_comment")) {
            komenterinp.setText(comment);
        }

        discount.setText(logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3));
        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);

        try {
            discountFist = Long.parseLong(discountText);
        } catch (NumberFormatException e) {
            // Handle the case where the expression cannot be evaluated
            FirebaseCrashlytics.getInstance().recordException(e);
        }


        btn_min = view.findViewById(R.id.btn_minus);
        btn_min.setOnClickListener(v -> {
            discountFist -= 5;
            if (discountFist <= MIN_VALUE) {
                discountFist = MIN_VALUE;
            }
            if(discountFist > 0) {
                discount.setText("+" + discountFist);
            } else {
                discount.setText( String.valueOf(discountFist));
            }
        });
        btn_plus = view.findViewById(R.id.btn_plus);
        btn_plus.setOnClickListener(v -> {
            discountFist += 5;
            if (discountFist >= MAX_VALUE) {
                discountFist = MAX_VALUE;
            }
            if(discountFist > 0) {
                discount.setText("+" + discountFist);
            } else {
                discount.setText( String.valueOf(discountFist));
            }
        });


        view.findViewById(R.id.okButton).setOnClickListener(v -> {
            onDismissBtn();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(requireActivity(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onDismissBtn();
            }
        });

        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        tvSelectedDate.setText(currentDate.format(formatter));

        tvSelectedDate.setOnClickListener(v -> showDataPickerDialog());

        return view;
    }



    // Метод для обновления отображаемой даты
    private void updateSelectedDate(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String formattedDate = sdf.format(calendar.getTime());
        tvSelectedDate.setText(formattedDate);
        sharedPreferencesHelperMain.saveValue("date", formattedDate);
    }

    public void onDismissBtn() {

        SparseBooleanArray booleanArray = listView.getCheckedItemPositions();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        for (int i = 0; i < booleanArray.size(); i++) {
            int position = booleanArray.keyAt(i);
            if (booleanArray.get(position)) {
                if (position >= 0 && position < arrayServiceCode.length) {
                    ContentValues cv = new ContentValues();
                    cv.put(arrayServiceCode[position], "1");

                    database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?", new String[]{"1"});
                } else {
                    Logger.e(context, TAG, "Индекс вне диапазона: " + position);
                }
            }
        }

        database.close();


        String commentText = komenterinp.getText().toString();
        if (!commentText.isEmpty()) {
            sharedPreferencesHelperMain.saveValue("comment", commentText);
            Logger.d(context, TAG, "comment " + commentText);
        }
        String discountText = discount.getText().toString();
        if (!discountText.isEmpty()) {

            sharedPreferencesHelperMain.saveValue("discount", discountText);

            ContentValues cv = new ContentValues();

            cv.put("discount", discountText);

            // обновляем по id
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();

        }
        //Проверка даты времени
        timeVerify();
        changeCost();
    }

    private void timeVerify() {
        String TAG = "TimeVerify"; // Тег для логирования
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // Текущие дата и время в Киеве
        LocalDateTime currentDateTimeInKyiv = LocalDateTime.now(ZoneId.of("Europe/Kiev"));
        LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/Kiev"));
        Logger.d(context, TAG, "Текущая дата и время в Киеве: " + currentDateTimeInKyiv);

        // Получение сохраненных значений
        String time = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String date = (String) sharedPreferencesHelperMain.getValue("date", "no_date");
        Logger.d(context, TAG, "Сохраненные значения -> время: " + time + ", дата: " + date);

        // Если дата не выбрана, используем текущую дату с экрана
        if (date.equals("no_date")) {
            date = tvSelectedDate.getText().toString();
            Logger.d(context, TAG, "Дата не выбрана, взята с экрана: " + date);
        }

        // Если время не выбрано, используем время с экрана
        if (time.equals("no_time")) {
            time = tvSelectedTime.getText().toString();
            Logger.d(context, TAG, "Время не выбрано, взято с экрана: " + time);
        }

        try {
            // Проверка, если дата или время все еще не определены
            if (date.equals("no_date") || time.equals("no_time")) {
                Logger.d(context, TAG, "Дата или время не определены, сброс значений");
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                return;
            }

            // Преобразование строки в LocalDateTime
            LocalDateTime dateTimeFromString = LocalDateTime.parse(date + " " + time, formatter);
            Logger.d(context, TAG, "Преобразованная дата и время: " + dateTimeFromString);

            // Проверка, если выбранное время раньше текущего
            if (dateTimeFromString.isBefore(currentDateTimeInKyiv)) {
                Logger.d(context, TAG, "Выбранное время в прошлом, сброс значений");
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                return;
            }

            // Вычисление разницы во времени
            long minutesDifference = Duration.between(currentDateTimeInKyiv, dateTimeFromString).toMinutes();
            Logger.d(context, TAG, "Разница во времени: " + minutesDifference + " минут");

            // Если разница меньше или равна 10 минут, сбрасываем
            if (minutesDifference <= 10 && minutesDifference >= 0) {
                Logger.d(context, TAG, "Разница <= 10 минут, сброс значений");
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
            } else {
                // Сохраняем валидные значения
                sharedPreferencesHelperMain.saveValue("time", time);
                sharedPreferencesHelperMain.saveValue("date", date);
                Logger.d(context, TAG, "Сохранены значения -> время: " + time + ", дата: " + date);
            }
        } catch (DateTimeParseException e) {
            Logger.e(context, TAG, "Ошибка парсинга даты/времени: " + e.getMessage());
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            Toast.makeText(context, "Неверный формат даты или времени", Toast.LENGTH_SHORT).show();
        } finally {
            database.close();
            Logger.d(context, TAG, "База данных закрыта");
        }
    }

    private void changeCost()  {
        String time = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String date = (String) sharedPreferencesHelperMain.getValue("date", "no_date");
        String comment = (String) sharedPreferencesHelperMain.getValue("comment", "no_comment");
        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");

        Logger.d(context, TAG, "changeCost: time " + time);
        Logger.d(context, TAG, "changeCost: comment " + comment);
        Logger.d(context, TAG, "changeCost: date " + date);
        Logger.d(context, TAG, "changeCost: tarif " + tarif);

        if (isAdded()) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.nav_visicom);
        } else {
            Logger.e(context,TAG, "Fragment не присоединён к активности — навигация невозможна");
        }
    }


    private void showDataPickerDialog() {

        Dialog dataPickerDialog = new Dialog(context);
        dataPickerDialog.setContentView(R.layout.custom_date_picker);

        NumberPicker npYear = dataPickerDialog.findViewById(R.id.npYear);
        NumberPicker npMonth = dataPickerDialog.findViewById(R.id.npMonth);
        NumberPicker npDay = dataPickerDialog.findViewById(R.id.npDay);

        float textSize = 24f; // Размер текста в sp

        setNumberPickerTextSize(npYear, textSize);
        setNumberPickerTextSize(npMonth, textSize);
        setNumberPickerTextSize(npDay, textSize);

        // Установка значений для NumberPicker
        npYear.setMinValue(2024);
        npYear.setMaxValue(2100);
        npYear.setValue(calendar.get(Calendar.YEAR));

        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(calendar.get(Calendar.MONTH) + 1);

        npDay.setMinValue(1);
        npDay.setMaxValue(getMaxDayOfMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)));
        npDay.setValue(calendar.get(Calendar.DAY_OF_MONTH));

        npMonth.setOnValueChangedListener((picker, oldVal, newVal) -> {
            npDay.setMaxValue(getMaxDayOfMonth(calendar.get(Calendar.YEAR), newVal - 1));
        });

        Button okButton = dataPickerDialog.findViewById(R.id.okButton);


        okButton.setOnClickListener(v -> {
            calendar.set(npYear.getValue(), npMonth.getValue() - 1, npDay.getValue());
            updateSelectedDate(calendar);
            dataPickerDialog.dismiss();
        });

        // Show the dialog
        dataPickerDialog.show();
    }

    private int getMaxDayOfMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void showTimePickerDialog() {
        // Initialize the dialog
        Dialog timePickerDialog = new Dialog(context);
        timePickerDialog.setContentView(R.layout.dialog_time_picker);

        // Initialize the NumberPickers
        NumberPicker hourPicker = timePickerDialog.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = timePickerDialog.findViewById(R.id.minutePicker);


        float textSize = 24f; // Размер текста в sp

        setNumberPickerTextSize(hourPicker, textSize);
        setNumberPickerTextSize(minutePicker, textSize);

        // Set the range for the hour and minute pickers
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        // Set the current time
        Calendar calendar = Calendar.getInstance();
        hourPicker.setValue(calendar.get(Calendar.HOUR_OF_DAY));
        Logger.d(context, TAG, "calendar.get(Calendar.HOUR_OF_DAY)" + calendar.get(Calendar.HOUR_OF_DAY));
        minutePicker.setValue(calendar.get(Calendar.MINUTE) +10);

        // Set the OK button click listener
        Button okButton = timePickerDialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> {
            int hourOfDay = hourPicker.getValue();
            int minute = minutePicker.getValue();

            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String formattedTime = sdf.format(calendar.getTime());
            Logger.d(context, TAG, "formattedTime: " + formattedTime);
            tvSelectedTime.setText(formattedTime);

            // Perform the required updates

            sharedPreferencesHelperMain.saveValue("time", formattedTime);

            timePickerDialog.dismiss();
        });

        // Show the dialog
        timePickerDialog.show();
    }



    private void setNumberPickerTextSize(NumberPicker numberPicker, float textSize) {
        try {
            // Найти все EditText внутри NumberPicker
            for (int i = 0; i < numberPicker.getChildCount(); i++) {
                View child = numberPicker.getChildAt(i);
                if (child instanceof EditText) {
                    ((EditText) child).setTextSize(textSize);
                    numberPicker.invalidate(); // Обновить NumberPicker
                }
            }
        } catch (Exception ignored) {

        }
    }


    private void updateSelectedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(calendar.getTime());
        Logger.d(context, TAG, "updateSelectedTime:formattedTime " + formattedTime);
        sharedPreferencesHelperMain.saveValue("time", formattedTime);
        tvSelectedTime.setText(formattedTime);
    }
    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = database.query(table, null, null, null, null, null, null);
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
        database.close();
        return list;
    }
}

