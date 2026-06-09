package com.taxi.easy.ua.ui.options_order;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.CustomListAdapter;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.sanitizer.InputSanitizerHelper;
import com.taxi.easy.ua.utils.ui.ScreenInsetsHelper;
import com.taxi.easy.ua.utils.worker.InclusiveTransportPreferenceWorker;
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
import com.taxi.easy.ua.utils.db.CursorReadHelper;

public class OptionsFragment extends Fragment {
    ListView listView;
    public String[] arrayService;
    public static String[] arrayServiceCode;
    private TextView tvSelectedTime, tvSelectedDate;
    private Calendar calendar;
    private EditText komenterinp, discount;
    Button btn_min, btn_plus;
    AppCompatButton btn_clear;
    long discountFist;
    final static long MIN_VALUE = -90;
    final static long MAX_VALUE = 200;

    private final String TAG = "OptionsFragment";
    TimeZone timeZone;
    SQLiteDatabase database;
    Activity context;
    private CheckBox cbInclusive;
    /** true только после явного выбора даты/времени в пикере (не дефолт +10 мин). */
    private boolean userPickedScheduledDateTime = false;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        View view = inflater.inflate(R.layout.fragment_visicom_options, container, false);
        listView = view.findViewById(R.id.list);
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }

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
        arrayServiceCode = DataArr.arrayServiceCode();

        CustomListAdapter adapterSet = new CustomListAdapter(context, arrayService, arrayService.length);
        listView.setAdapter(adapterSet);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        cbInclusive = view.findViewById(R.id.cb_inclusive);
        komenterinp = view.findViewById(R.id.komenterinp);
// Загрузка сохраненного состояния инклюзивности
// Метод needsInclusiveTransport() возвращает boolean, по умолчанию false
        boolean isInclusive = InclusiveTransportPreferenceWorker.needsInclusiveTransport();
        cbInclusive.setChecked(isInclusive);
        if(isInclusive) {
            komenterinp.setText(context.getString(R.string.inclusive_transport_message_yes));
        }


// Обработчик изменения состояния
        cbInclusive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            InclusiveTransportPreferenceWorker.saveUserPreference(isChecked);
            if(!isChecked) {
                komenterinp.setText("");
                sharedPreferencesHelperMain.saveValue("comment", "no_comment");
            } else {
                // Санитизация текста перед установкой
                String inclusiveMessage = context.getString(R.string.inclusive_transport_message_yes);
                String safeMessage = InputSanitizerHelper.sanitize(inclusiveMessage, InputSanitizerHelper.InputType.COMMENT);
                komenterinp.setText(safeMessage);
            }
            Logger.d(context, TAG, "Инклюзивность: " + (isChecked ? "включена" : "выключена"));
        });

        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        for (int i = 0; i < arrayServiceCode.length; i++) {
            if(services.get(i+1).equals("1")) {
                listView.setItemChecked(i,true);
            }
        }

        String[] tariffArr = new String[]{
                context.getResources().getString(R.string.start_t),
//                context.getResources().getString(R.string.base_onl_t),
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

        String tariffOld = (String) sharedPreferencesHelperMain.getValue("tarif", " ");

        switch (tariffOld) {
            case  "Базовый":
                spinner.setSelection(1);
                break;
            case "Универсал":
                spinner.setSelection(2);
                break;
            case "Бизнес-класс":
                spinner.setSelection(3);
                break;
            case "Премиум-класс":
                spinner.setSelection(4);
                break;
            case "Эконом-класс":
                spinner.setSelection(5);
                break;
            case "Микроавтобус":
                spinner.setSelection(6);
                break;
            default:
                spinner.setSelection(0);
        }

        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
        String city = stringList.get(1);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tariff_to_server;
                Logger.d(context, TAG, "onItemSelected: position" + position);
                switch (position) {
                    case 1:
                        tariff_to_server = "Базовый";
                        break;
                    case 2:
                        tariff_to_server = "Универсал";
                        break;
                    case 3:
                        tariff_to_server = "Бизнес-класс";
                        break;
                    case 4:
                        tariff_to_server = "Премиум-класс";
                        break;
                    case 5:
                        tariff_to_server = "Эконом-класс";
                        break;
                    case 6:
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
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        timeZone = TimeZone.getDefault();
        restoreDateTimeFieldsFromPrefs();
        tvSelectedTime.setOnClickListener(v -> showTimePickerDialog());

        discount = view.findViewById(R.id.discinp);

        String comment = (String) sharedPreferencesHelperMain.getValue("comment", "no_comment");

        if (!comment.equals("no_comment")) {
            komenterinp.setText(comment);
        }

        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
        try {
            discountFist = parseDiscountPercent(discountText);
        } catch (NumberFormatException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            discountFist = 0;
        }
        updateDiscountDisplay();

        btn_min = view.findViewById(R.id.btn_minus);
        btn_min.setOnClickListener(v -> {
            discountFist -= 5;
            if (discountFist <= MIN_VALUE) {
                discountFist = MIN_VALUE;
            }
            updateDiscountDisplay();
        });
        btn_plus = view.findViewById(R.id.btn_plus);
        btn_plus.setOnClickListener(v -> {
            discountFist += 5;
            if (discountFist >= MAX_VALUE) {
                discountFist = MAX_VALUE;
            }
            updateDiscountDisplay();
        });

        // Initialize Clear Button
        btn_clear = view.findViewById(R.id.btn_clear);
        btn_clear.setOnClickListener(v -> clearAllData());

        view.findViewById(R.id.okButton).setOnClickListener(v -> {
            onDismissBtn();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(requireActivity(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onDismissBtn();
            }
        });

        tvSelectedDate.setOnClickListener(v -> showDataPickerDialog());
        setupSanitizers();
        ScreenInsetsHelper.applyScrollableScreenPadding(view);
        listView.setClipToPadding(false);
        return view;
    }
    private void setupSanitizers() {
        // Санитизация комментария
        komenterinp.addTextChangedListener(new TextWatcher() {
            private boolean isSanitizing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isSanitizing) return;

                String original = s.toString();
                String sanitized = InputSanitizerHelper.sanitize(original, InputSanitizerHelper.InputType.COMMENT);

                // Проверка на SQL-инъекции
                if (InputSanitizerHelper.containsSqlInjectionPatterns(original)) {
                    sanitized = "";
                    Toast.makeText(context, R.string.invalid_input_detected, Toast.LENGTH_SHORT).show();
                }

                if (!sanitized.equals(original)) {
                    isSanitizing = true;
                    s.replace(0, s.length(), sanitized);
                    isSanitizing = false;
                }
            }
        });

        discount.addTextChangedListener(new TextWatcher() {
            private boolean isSanitizing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isSanitizing) return;

                String original = s.toString();
                String sanitized = sanitizeDiscountInput(original);

                if (!sanitized.equals(original)) {
                    isSanitizing = true;
                    s.replace(0, s.length(), sanitized);
                    isSanitizing = false;
                }
                syncDiscountFistFromInput(sanitized);
            }
        });
    }

    private void updateDiscountDisplay() {
        if (discount != null) {
            discount.setText(formatDiscountPercent(discountFist));
        }
    }

    private static String formatDiscountPercent(long value) {
        if (value > 0) {
            return "+" + value;
        }
        return String.valueOf(value);
    }

    private static long parseDiscountPercent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return Long.parseLong(text.replace("+", "").trim());
    }

    private static String sanitizeDiscountInput(String original) {
        if (original == null) {
            return "";
        }
        String trimmed = original.trim();
        boolean negative = trimmed.startsWith("-");
        boolean positive = !negative && trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^\\d]", "");
        if (negative) {
            return digits.isEmpty() ? "-" : "-" + digits;
        }
        if (positive) {
            return digits.isEmpty() ? "+" : "+" + digits;
        }
        return digits;
    }

    private void syncDiscountFistFromInput(String text) {
        if (text == null || text.isEmpty() || text.equals("+") || text.equals("-")) {
            return;
        }
        try {
            discountFist = parseDiscountPercent(text);
        } catch (NumberFormatException ignored) {
        }
    }

    private void clearAllData() {
        // Clear ListView selections (services)
        listView.clearChoices();
        for (int i = 0; i < arrayServiceCode.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(arrayServiceCode[i], "0");
            database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?", new String[]{"1"});
        }
        listView.setAdapter(new CustomListAdapter(context, arrayService, arrayService.length));

        // Clear tariff selection
        Spinner spinner = getView().findViewById(R.id.list_tariff);
        spinner.setSelection(0);
        sharedPreferencesHelperMain.saveValue("tarif", " ");

        // Clear comment (с санитизацией)
        komenterinp.setText("");
        sharedPreferencesHelperMain.saveValue("comment", "no_comment");

        // Clear discount (с санитизацией)
        discountFist = 0;
        updateDiscountDisplay();
        ContentValues cv = new ContentValues();
        cv.put("discount", "0");
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?", new String[]{"1"});

        // Clear date and time
        initDefaultDateTimeUi();
        sharedPreferencesHelperMain.saveValue("time", "no_time");
        sharedPreferencesHelperMain.saveValue("date", "no_date");

        Toast.makeText(context, R.string.all_data_clear, Toast.LENGTH_SHORT).show();
        Logger.d(context, TAG, "All data cleared");
        cbInclusive.setChecked(false);
        InclusiveTransportPreferenceWorker.saveUserPreference(false);
    }

    private void restoreDateTimeFieldsFromPrefs() {
        String savedTime = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String savedDate = (String) sharedPreferencesHelperMain.getValue("date", "no_date");
        if ("no_time".equals(savedTime) || "no_date".equals(savedDate)) {
            initDefaultDateTimeUi();
            return;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            LocalDateTime scheduled = LocalDateTime.parse(savedDate + " " + savedTime, formatter);
            calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, scheduled.getYear());
            calendar.set(Calendar.MONTH, scheduled.getMonthValue() - 1);
            calendar.set(Calendar.DAY_OF_MONTH, scheduled.getDayOfMonth());
            calendar.set(Calendar.HOUR_OF_DAY, scheduled.getHour());
            calendar.set(Calendar.MINUTE, scheduled.getMinute());
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            tvSelectedDate.setText(savedDate);
            tvSelectedTime.setText(savedTime);
            userPickedScheduledDateTime = true;
        } catch (DateTimeParseException e) {
            Logger.e(context, TAG, "restoreDateTimeFieldsFromPrefs: " + e.getMessage());
            initDefaultDateTimeUi();
        }
    }

    private void initDefaultDateTimeUi() {
        userPickedScheduledDateTime = false;
        calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);
        updateSelectedTime();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        tvSelectedDate.setText(currentDate.format(formatter));
    }

    private void updateSelectedDate(Calendar targetCalendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String formattedDate = sdf.format(targetCalendar.getTime());
        tvSelectedDate.setText(formattedDate);
    }

    public void onDismissBtn() {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SparseBooleanArray checkedItems = listView.getCheckedItemPositions();

        for (int i = 0; i < checkedItems.size(); i++) {
            int position = checkedItems.keyAt(i);
            if (position >= 0 && position < arrayServiceCode.length) {
                ContentValues cv = new ContentValues();
                cv.put(arrayServiceCode[position], checkedItems.get(position) ? "1" : "0");
                database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?", new String[]{"1"});
            } else {
                Logger.e(context, TAG, "Индекс вне диапазона: " + position);
            }
        }

        database.close();

        // САНИТИЗАЦИЯ комментария перед сохранением
        String commentText = komenterinp.getText().toString();
        String safeComment = InputSanitizerHelper.sanitize(commentText, InputSanitizerHelper.InputType.COMMENT);
        safeComment = InputSanitizerHelper.escapeSql(safeComment);

        if (!safeComment.isEmpty()) {
            sharedPreferencesHelperMain.saveValue("comment", safeComment);
            Logger.d(context, TAG, "comment " + safeComment);
        } else {
            sharedPreferencesHelperMain.saveValue("comment", "no_comment");
        }

        // Сохраняем числовое значение скидки (со знаком), не текст поля: NUMERIC-санитайзер отрезает «-»
        String safeDiscount = String.valueOf(discountFist);
        sharedPreferencesHelperMain.saveValue("discount", safeDiscount);

        ContentValues cv = new ContentValues();
        cv.put("discount", safeDiscount);
        database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?", new String[]{"1"});
        database.close();

        timeVerify();
        changeCost();
    }

    private void timeVerify() {
        String TAG = "TimeVerify";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        LocalDateTime currentDateTimeInKyiv = LocalDateTime.now(ZoneId.of("Europe/Kiev"));
        Logger.d(context, TAG, "Текущая дата и время в Киеве: " + currentDateTimeInKyiv);

        // Всегда берем актуальные значения с экрана
        String time = tvSelectedTime.getText().toString();
        String date = tvSelectedDate.getText().toString();
        Logger.d(context, TAG, "Текущие значения с экрана -> время: " + time + ", дата: " + date);

        try {
            LocalDateTime dateTimeFromString = LocalDateTime.parse(date + " " + time, formatter);
            Logger.d(context, TAG, "Преобразованная дата и время: " + dateTimeFromString);

            long minutesDifference = Duration.between(currentDateTimeInKyiv, dateTimeFromString).toMinutes();
            Logger.d(context, TAG, "Разница во времени: " + minutesDifference + " минут");

            if (dateTimeFromString.isBefore(currentDateTimeInKyiv)) {
                Logger.d(context, TAG, "Выбранное время в прошлом, сброс значений");
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                userPickedScheduledDateTime = false;
            } else if (!userPickedScheduledDateTime || minutesDifference < 10) {
                Logger.d(context, TAG, "Заказ без отложки (сейчас), без тоста");
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
            } else {
                sharedPreferencesHelperMain.saveValue("time", time);
                sharedPreferencesHelperMain.saveValue("date", date);
                Logger.d(context, TAG, "Сохранены значения -> время: " + time + ", дата: " + date);
            }
        } catch (DateTimeParseException e) {
            Logger.e(context, TAG, "Ошибка парсинга даты/времени: " + e.getMessage());
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            Toast.makeText(context, "Неверный формат даты или времени", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeCost()  {
        String time = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String date = (String) sharedPreferencesHelperMain.getValue("date", "no_date");
        String comment = (String) sharedPreferencesHelperMain.getValue("comment", "no_comment");
        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
        String inclusive = (String) sharedPreferencesHelperMain.getValue("inclusive", "0");
        Logger.d(context, TAG, "changeCost: inclusive " + inclusive);

        Logger.d(context, TAG, "changeCost: time " + time);
        Logger.d(context, TAG, "changeCost: comment " + comment);
        Logger.d(context, TAG, "changeCost: date " + date);
        Logger.d(context, TAG, "changeCost: tarif " + tarif);

        String initial_page = (String) sharedPreferencesHelperMain.getValue("initial_page", "visicom");


        if (isAdded()) {
            if(initial_page.equals("visicom")){
                // Проверяем, существует ли VisicomFragment и его views
                Fragment visicomFragment = getParentFragmentManager().findFragmentByTag("visicom");
                if (visicomFragment != null && visicomFragment.isAdded() && visicomFragment.getView() != null) {
                    VisicomFragment.tariffBtnColor();
                }
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.nav_visicom);
            } else {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.nav_home);
            }

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

        float textSize = 24f;

        setNumberPickerTextSize(npYear, textSize);
        setNumberPickerTextSize(npMonth, textSize);
        setNumberPickerTextSize(npDay, textSize);

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
            userPickedScheduledDateTime = true;
            dataPickerDialog.dismiss();
        });

        dataPickerDialog.show();
    }

    private int getMaxDayOfMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void showTimePickerDialog() {
        Dialog timePickerDialog = new Dialog(context);
        timePickerDialog.setContentView(R.layout.dialog_time_picker);

        NumberPicker hourPicker = timePickerDialog.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = timePickerDialog.findViewById(R.id.minutePicker);

        float textSize = 24f;

        setNumberPickerTextSize(hourPicker, textSize);
        setNumberPickerTextSize(minutePicker, textSize);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        // Убедитесь, что используем поле класса this.calendar
        hourPicker.setValue(this.calendar.get(Calendar.HOUR_OF_DAY));
        Logger.d(context, TAG, "calendar.get(Calendar.HOUR_OF_DAY)" + this.calendar.get(Calendar.HOUR_OF_DAY));
        minutePicker.setValue(this.calendar.get(Calendar.MINUTE));

        Button okButton = timePickerDialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> {
            int hourOfDay = hourPicker.getValue();
            int minute = minutePicker.getValue();

            this.calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            this.calendar.set(Calendar.MINUTE, minute);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String formattedTime = sdf.format(this.calendar.getTime());
            Logger.d(context, TAG, "formattedTime: " + formattedTime);
            tvSelectedTime.setText(formattedTime);
            userPickedScheduledDateTime = true;
            timePickerDialog.dismiss();
        });

        timePickerDialog.show();
    }

    private void setNumberPickerTextSize(NumberPicker numberPicker, float textSize) {
        try {
            for (int i = 0; i < numberPicker.getChildCount(); i++) {
                View child = numberPicker.getChildAt(i);
                if (child instanceof EditText) {
                    ((EditText) child).setTextSize(textSize);
                    numberPicker.invalidate();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void updateSelectedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(calendar.getTime());
        Logger.d(context, TAG, "updateSelectedTime:formattedTime " + formattedTime);
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
                    str = str.concat(cn + " = " + CursorReadHelper.getString(c, cn) + "; ");
                    list.add(CursorReadHelper.getString(c, cn));
                }
            } while (c.moveToNext());
        }
        database.close();
        return list;
    }
}