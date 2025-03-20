package com.taxi.easy.ua.utils.bottom_sheet;


import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.navController;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import androidx.core.content.FileProvider;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.cities.api.CityApiClient;
import com.taxi.easy.ua.ui.home.cities.api.CityResponse;
import com.taxi.easy.ua.ui.home.cities.api.CityService;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.helpers.TelegramUtils;
import com.taxi.easy.ua.utils.log.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MyBottomSheetErrorFragment extends BottomSheetDialogFragment {
    private static final String TAG = "MyBottomSheetErrorFragment";
    TextView textViewInfo;
    AppCompatButton btn_help, btn_ok;
    String errorMessage;
    final String Kyiv_City_phone = "tel:0674443804";
    final String Dnipropetrovsk_Oblast_phone = "tel:0667257070";
    final String Odessa_phone = "tel:0737257070";
    final String Zaporizhzhia_phone = "tel:0687257070";
    final String Cherkasy_Oblast_phone = "tel:0962294243";
    String phoneNumber;
    public MyBottomSheetErrorFragment() {
    }

    public MyBottomSheetErrorFragment(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    // Публичный безаргументный конструктор

     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_list_layout, container, false);

        setCancelable(false);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(v -> {
            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = stringList.get(3);

            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        btn_ok = view.findViewById(R.id.btn_ok);

        textViewInfo = view.findViewById(R.id.textViewInfo);
        Logger.d(getActivity(), TAG, "onCreateView:errorMessage " + errorMessage);
        if (errorMessage != null && !errorMessage.equals("null")) {
            textViewInfo.setText(errorMessage);
            if (errorMessage.equals(getString(R.string.verify_internet))
                || errorMessage.equals(getString(R.string.error_message))
            ) {
                btn_ok.setVisibility(View.GONE);
                textViewInfo.setOnClickListener(v -> {
                    SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    ContentValues cv = new ContentValues();

                    cv.put("email", "email");

                    // обновляем по id
                    database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                            new String[] { "1" });
                    database.close();
                    dismiss();

                    if (NetworkUtils.isNetworkAvailable(requireContext())) {

                        MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build());
                    }
//                    startActivity(new Intent(requireContext(), MainActivity.class));
                });
            } else if(errorMessage.equals(getString(R.string.server_error_connected))) {
                textViewInfo.setOnClickListener(v -> dismiss());
                btn_ok.setText(getString(R.string.send_email_admin));
                btn_ok.setOnClickListener(v -> {
                    sendEmailAdmin (errorMessage);
                    dismiss();
                });


                String logFilePath = requireActivity().getExternalFilesDir(null) + "/app_log.txt"; // Путь к лог-файлу
                TelegramUtils.sendErrorToTelegram(generateEmailBody(errorMessage), logFilePath);
//                TelegramUtils.sendLogFileToTelegram(logFilePath);



            } else if (errorMessage.equals(getString(R.string.order_to_cancel_true))){
                textViewInfo.setOnClickListener(v -> dismiss());
                btn_ok.setText(getString(R.string.order_to_cancel_review));
                btn_ok.setOnClickListener(v -> {
                    navController.navigate(R.id.nav_cancel, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_visicom, true)
                            .build());


                    dismiss();
                });
            } else if (errorMessage.equals(getString(R.string.black_list_message))){
                textViewInfo.setOnClickListener(v -> dismiss());
                btn_ok.setText(getString(R.string.ok_error));
                btn_ok.setOnClickListener(v -> {
                    NavDestination currentDestination = navController.getCurrentDestination();

                    if (currentDestination == null || currentDestination.getId() != R.id.nav_visicom) {
                        navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build());
                    }

                    dismiss();
                });
            } else if (errorMessage.equals(getString(R.string.no_cards_info))){
                 textViewInfo.setOnClickListener(view2 -> {
                     dismiss();
//                     Intent intent = new Intent(getActivity(), MainActivity.class);
//                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                     startActivity(intent);
                 });
                btn_ok.setText(getString(R.string.link_card));
                btn_ok.setOnClickListener(v -> {
                        navController.navigate(R.id.nav_card, null, new NavOptions.Builder().build());
                    dismiss();

                });
            } else {
                btn_ok.setOnClickListener(v -> dismiss());
            }
        } else {
            assert errorMessage != null;
            if (errorMessage.equals(getString(R.string.verify_address))){
                     textViewInfo.setOnClickListener(view1 -> {
                         List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
                         String city = stringList.get(1);
                         updateMyPosition(city);
                         restartApplication(requireActivity());
                     });
                    btn_ok.setText(getString(R.string.ok_error));
                    btn_ok.setOnClickListener(v -> {
                        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
                        String city = stringList.get(1);
                        updateMyPosition(city);
                        restartApplication(requireActivity());

                    });
            } else {
                textViewInfo.setText(getString(R.string.error_message));
                btn_ok.setText(getString(R.string.try_again));
                btn_ok.setOnClickListener(v -> startActivity(new Intent(requireContext(), MainActivity.class)));
            }
        }

        return view;
    }

    private void restartApplication(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);
        }

        // Завершаем процесс
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
    private void updateMyPosition(String city) {

        double startLat;
        double startLan;
        String position;
        Logger.d(getActivity(), TAG, "updateMyPosition:city "+ city);

        switch (city) {
            case "Kyiv City":
                position = getString(R.string.pos_k);
                startLat = 50.451107;
                startLan = 30.524907;
                phoneNumber = Kyiv_City_phone; // Здесь также добавляем номер телефона
                break;
            case "Dnipropetrovsk Oblast":
                // Днепр
                position = getString(R.string.pos_d);
                startLat = 48.4647;
                startLan = 35.0462;
                phoneNumber = Dnipropetrovsk_Oblast_phone; // Укажите соответствующий номер телефона
                break;
            case "Odessa":
                position = getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                phoneNumber = Odessa_phone;
                break;
            case "Zaporizhzhia":
                position = getString(R.string.pos_z);
                startLat = 47.84015;
                startLan = 35.13634;
                phoneNumber = Zaporizhzhia_phone;
                break;
            case "Cherkasy Oblast":
                position = getString(R.string.pos_c);
                startLat = 49.44469;
                startLan = 32.05728;
                phoneNumber = Cherkasy_Oblast_phone;
                break;
            case "Lviv":
                position = getString(R.string.pos_l);
                startLat = 49.83993;
                startLan = 24.02973;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ivano_frankivsk":
                position = getString(R.string.pos_if);
                startLat = 48.92005;
                startLan = 24.71067;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Vinnytsia":
                position = getString(R.string.pos_v);
                startLat = 49.23325;
                startLan = 28.46865;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Poltava":
                position = getString(R.string.pos_p);
                startLat = 49.59325;
                startLan = 34.54938;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Sumy":
                position = getString(R.string.pos_s);
                startLat = 50.90775;
                startLan = 34.79865;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kharkiv":
                position = getString(R.string.pos_h);
                startLat = 49.99358;
                startLan = 36.23191;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Chernihiv":
                position = getString(R.string.pos_ch);
                startLat = 51.4933;
                startLan = 31.2972;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Rivne":
                position = getString(R.string.pos_r);
                startLat = 50.6198;
                startLan = 26.2406;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ternopil":
                position = getString(R.string.pos_t);
                startLat = 49.54479;
                startLan = 25.5990;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Khmelnytskyi":
                position = getString(R.string.pos_kh);
                startLat = 49.41548;
                startLan = 27.00674;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;

            case "Zakarpattya":
                position = getString(R.string.pos_uz);
                startLat = 48.61913;
                startLan = 22.29475;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Zhytomyr":
                position = getString(R.string.pos_zt);
                startLat = 50.26801;
                startLan = 28.68026;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kropyvnytskyi":
                position = getString(R.string.pos_kr);
                startLat = 48.51159;
                startLan = 32.26982;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Mykolaiv":
                position = getString(R.string.pos_m);
                startLat = 46.97498;
                startLan = 31.99378;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Сhernivtsi":
                position = getString(R.string.pos_chr);
                startLat = 48.29306;
                startLan = 25.93484;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Lutsk":
                position = getString(R.string.pos_ltk);
                startLat = 50.73968;
                startLan = 25.32400;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "OdessaTest":
                position = getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                phoneNumber = Kyiv_City_phone;
                break;

            default:
                position = getString(R.string.pos_f);
                startLat = 52.13472;
                startLan = 21.00424;
                phoneNumber = Kyiv_City_phone; // Номер телефона по умолчанию
                break;
        }

        cityMaxPay(city);
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        ContentValues cv = new ContentValues();

        cv.put("city", city);
        cv.put("phone", phoneNumber);
        database.update(MainActivity.CITY_INFO, cv, "id = ?", new String[]{"1"});

        cv = new ContentValues();
        cv.put("startLat", startLat);
        cv.put("startLan", startLan);
        cv.put("position", position);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[] { "1" });

        cv = new ContentValues();
        cv.put("tarif", " ");
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });

        cv = new ContentValues();
        cv.put("payment_type", "nal_payment");

        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();

        List<String> settings = new ArrayList<>();

        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(position);
        settings.add(position);

        updateRoutMarker(settings);

    }

    private void updateRoutMarker(List<String> settings) {
        Logger.d(requireActivity(), TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));

        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();

    }
    private void cityMaxPay(String city) {


        String BASE_URL =sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        CityApiClient cityApiClient = new CityApiClient(BASE_URL);
        CityService cityService = cityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues(city, getString(R.string.application));

        call.enqueue(new Callback<CityResponse>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call, @NonNull Response<CityResponse> response) {
                if (response.isSuccessful()) {
                    CityResponse cityResponse = response.body();
                    if (cityResponse != null) {
                        int cardMaxPay = cityResponse.getCardMaxPay();
                        int bonusMaxPay = cityResponse.getBonusMaxPay();
                        String black_list = cityResponse.getBlack_list();

                        ContentValues cv = new ContentValues();
                        cv.put("card_max_pay", cardMaxPay);
                        cv.put("bonus_max_pay", bonusMaxPay);
                        sharedPreferencesHelperMain.saveValue("black_list", black_list);

                        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                new String[]{"1"});

                        database.close();




                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Logger.d(requireActivity(), TAG, "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                Logger.d(requireActivity(), TAG, "Failed. Error message: " + t.getMessage());
            }
        });
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
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
        c.close();
        return list;
    }

    @SuppressLint("IntentReset")
    private void sendEmailAdmin (String errorMessage) {

        String subject = getString(R.string.SA_subject) + generateRandomString(10);

        String body = generateEmailBody(errorMessage);


        String[] TO = {"taxi.easy.ua@gmail.com"};

        File logFile = new File(requireActivity().getExternalFilesDir(null), "app_log.txt");

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (logFile.exists()) {
            Uri uri = FileProvider.getUriForFile(requireActivity(), requireActivity().getPackageName() + ".fileprovider", logFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            Logger.e(requireActivity(), "MyBottomSheetErrorFragment", "Log file does not exist");
        }
        try {
            startActivity(Intent.createChooser(emailIntent, subject));
        } catch (android.content.ActivityNotFoundException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }


    }

    public String generateEmailBody(String errorMessage) {

        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO, requireActivity());


        // Определение города

        String city = switch (stringList.get(1)) {
            case "Dnipropetrovsk Oblast" -> getString(R.string.Dnipro_city);
            case "Zaporizhzhia" -> getString(R.string.Zaporizhzhia);
            case "Cherkasy Oblast" -> getString(R.string.Cherkasy);
            case "Odessa" -> getString(R.string.Odessa);
            case "OdessaTest" -> getString(R.string.OdessaTest);
            default -> getString(R.string.Kyiv_city);
        };

        // Формирование тела сообщения

        return errorMessage + "\n"+
                getString(R.string.SA_info_pas) + "\n" +
                getString(R.string.SA_info_city) + " " + city + "\n" +
                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
                getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
                getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n" + "\n";
    }


    private String generateRandomString(int length) {
        String characters = "012345678901234567890123456789";
        StringBuilder randomString = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomString.append(randomChar);
        }

        return randomString.toString();
    }

}

