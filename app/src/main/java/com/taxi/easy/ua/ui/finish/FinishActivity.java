package com.taxi.easy.ua.ui.finish;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.MyBottomSheetBlackListFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FinishActivity extends AppCompatActivity {
    private TextView text_status;
    String api;
    String baseUrl = "https://m.easy-order-taxi.site";
    Map<String, String> receivedMap;
    String UID_key;
    Thread thread;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);
        new VerifyUserTask().execute();
        List<String> stringListArr = logCursor(MainActivity.CITY_INFO);
        switch (stringListArr.get(1)){
            case "Kyiv City":
                api = MainActivity.apiKyiv;
                break;
            case "Dnipropetrovsk Oblast":
                api = MainActivity.apiDnipro;
                break;
            case "Odessa":
                api = MainActivity.apiOdessa;
                break;
            case "Zaporizhzhia":
                api = MainActivity.apiZaporizhzhia;
                break;
            case "Cherkasy Oblast":
                api = MainActivity.apiCherkasy;
                break;
            case "OdessaTest":
                api = MainActivity.apiTest;
                break;
            default:
                api = MainActivity.apiKyiv;
                break;
        }
        String parameterValue = getIntent().getStringExtra("messageResult_key");
        String parameterCost = getIntent().getStringExtra("messageCost_key");
        receivedMap = (HashMap<String, String>) getIntent().getSerializableExtra("sendUrlMap");

        Log.d("TAG", "onCreate: receivedMap" + receivedMap.toString());
        TextView text_full_message = findViewById(R.id.text_full_message);
        text_full_message.setText(parameterValue);

        UID_key = getIntent().getStringExtra("UID_key");

        text_status = findViewById(R.id.text_status);
        statusOrderWithDifferentValue(UID_key);


        Button btn_reset_status = findViewById(R.id.btn_reset_status);
        btn_reset_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    statusOrderWithDifferentValue(UID_key);
                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        });

        Button btn_cancel_order = findViewById(R.id.btn_cancel_order);
        long delayMillis = 5 * 60 * 1000;

        Handler handler = new Handler();

        List<String> stringList = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String bonusPayment =  stringList.get(4);

         if (bonusPayment.equals("bonus_payment")) {
             String baseUrl = "https://m.easy-order-taxi.site";
             String url = baseUrl + "/bonusBalance/recordsBloke/" + UID_key;
             Log.d("TAG", "onCreate: doubleOrder):  " +receivedMap.get("doubleOrder") );


             fetchBonus(url);
             handler.postDelayed(new Runnable() {
                 @Override
                 public void run() {
                     btn_cancel_order.setVisibility(View.INVISIBLE);
                 }
             }, delayMillis);
         }
        btn_cancel_order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    cancelOrderWithDifferentValue(UID_key);
                    if(!receivedMap.get("dispatching_order_uid_Double").equals(" ")) {
                        cancelOrderWithDifferentValue(receivedMap.get("dispatching_order_uid_Double"));
                    }
                    if (thread != null && thread.isAlive()) {
                        thread.interrupt();
                    }
                } else {
                    text_status.setText(R.string.verify_internet);
                }
            }
        });

        Button btn_again = findViewById(R.id.btn_again);
        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!verifyOrder()) {
                    MyBottomSheetBlackListFragment bottomSheetDialogFragment = new MyBottomSheetBlackListFragment("orderCost");
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                } else {
                    if(connected()){
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    } else {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                }
            }
        });

        Button btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });
        FloatingActionButton fab_cal = findViewById(R.id.fab_call);
        fab_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone;
                List<String> stringList = logCursor(MainActivity.CITY_INFO);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<String> stringList = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String bonusPayment =  stringList.get(4);
        if (bonusPayment.equals("bonus_payment")) {
           thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Здесь вызывайте вашу функцию fetchCarFound()
                    fetchCarFound();
                }
            });
           thread.start();
        }
    }

    private void fetchBonus(String url) {

        Call<BonusResponse> call = ApiClient.getApiService().getBonus(url);
        Log.d("TAG", "fetchBonus: " + url);
        call.enqueue(new Callback<BonusResponse>() {
            @Override
            public void onResponse(Call<BonusResponse> call, Response<BonusResponse> response) {
                BonusResponse bonusResponse = response.body();
                if (response.isSuccessful()) {

                    String bonus = String.valueOf(bonusResponse.getBonus());
                    String message = getString(R.string.block_mes) + " " + bonus + " " + getString(R.string.bon);

                    MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(Call<BonusResponse> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                // Дополнительная обработка ошибки
            }
        });
    }
    private void fetchCarFound() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl) // Замените BASE_URL на ваш базовый URL сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();

// Создайте экземпляр ApiService
        ApiService apiService = retrofit.create(ApiService.class);

// Вызов метода startNewProcessExecutionStatus с передачей параметров
        Call<Void> call = apiService.startNewProcessExecutionStatus(
                receivedMap.get("doubleOrder")
        );
        String url = call.request().url().toString();
        Log.d("TAG", "URL запроса: " + url);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Обработайте ошибку при выполнении запроса
            }
        });

    }

    private boolean verifyOrder() {
        SQLiteDatabase database = this.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO).get(1).equals("0")) {
                verify = false;
            }
            cursor.close();
        }
        database.close();
        return verify;
    }
    private boolean connected() {

        boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            hasConnect = true;
        }

        return hasConnect;
    }
    @SuppressLint("Range")
    private List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = this.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
    private void cancelOrderWithDifferentValue(String value) {

        String url = baseUrl + "/" + api + "/android/webordersCancel/" + value;
        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Log.d("TAG", "cancelOrderWithDifferentValue cancelOrderUrl: " + url);
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    if (status != null) {
                        String result =  String.valueOf(status.getResponse());
                        text_status.setText(result); // Установите текстовое значение в text_status
                    } else {
                        text_status.setText(R.string.verify_internet);
                    }
                } else {
                    // Обработка неуспешного ответа
                    text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d("TAG", "onFailure: " + errorMessage);
                text_status.setText(R.string.verify_internet);
            }
        });
    }

    private void statusOrderWithDifferentValue(String value) {
        String url = baseUrl + "/" + api + "/android/historyUIDStatus/" + value;

        Call<OrderResponse> call = ApiClient.getApiService().statusOrder(url);
        Log.d("TAG", "cancelOrderWithDifferentValue cancelOrderUrl: " + url);

        // Выполняем запрос асинхронно
        call.enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (response.isSuccessful()) {
                    // Получаем объект OrderResponse из успешного ответа
                    OrderResponse orderResponse = response.body();

                    // Далее вы можете использовать полученные данные из orderResponse
                    // например:
                    assert orderResponse != null;
                    String executionStatus = orderResponse.getExecutionStatus();
                    String orderCarInfo = orderResponse.getOrderCarInfo();
                    String driverPhone = orderResponse.getDriverPhone();
                    String requiredTime = orderResponse.getRequiredTime();
                    if (requiredTime != null && !requiredTime.isEmpty()) {
                        requiredTime = formatDate (orderResponse.getRequiredTime());
                    }


                    String message;
                    // Обработка различных вариантов executionStatus
                    switch (executionStatus) {
                        case "WaitingCarSearch":
                            message = getString(R.string.ex_st_1);
                            break;
                        case "SearchesForCar":
                            message = getString(R.string.ex_st_0);
                            break;
                        case "CarFound":
                            // Формируем сообщение с учетом возможных пустых значений переменных
                            StringBuilder messageBuilder = new StringBuilder(getString(R.string.ex_st_2));

                            if (orderCarInfo != null && !orderCarInfo.isEmpty()) {
                                messageBuilder.append(getString(R.string.ex_st_3)).append(orderCarInfo);
                            }

                            if (driverPhone != null && !driverPhone.isEmpty()) {
                                messageBuilder.append(getString(R.string.ex_st_4)).append(driverPhone);
                            }

                            if (requiredTime != null && !requiredTime.isEmpty()) {
                                messageBuilder.append(getString(R.string.ex_st_5)).append(requiredTime);
                            }

                            message = messageBuilder.toString();
                            break;
                        default:
                            message = getString(R.string.def_status);
                            break;
                    }

                    text_status.setText(message);

                } else {
                    text_status.setText(getString(R.string.def_status));
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                text_status.setText(getString(R.string.def_status));
            }
        });
    }

    private String formatDate (String requiredTime) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        // Формат для вывода в украинской локализации
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("uk", "UA"));
        // Преобразуем строку в объект Date
        Date date = null;
        try {
            date = inputFormat.parse(requiredTime);
        } catch (ParseException e) {
            Log.d("TAG", "onCreate:" + new RuntimeException(e));
        }

        // Форматируем дату и время в украинском формате
        return outputFormat.format(date);

    }

    public class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
        private Exception exception;
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);

            String url = "https://m.easy-order-taxi.site/android/verifyBlackListUser/" + userEmail + "/" + "com.taxi.easy.ua";
            try {
                return CostJSONParser.sendURL(url);
            } catch (Exception e) {
                exception = e;
                return null;
            }

        }

        @Override
        protected void onPostExecute(Map<String, String> sendUrlMap) {
            String message = sendUrlMap.get("message");
            ContentValues cv = new ContentValues();
            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            if (message != null) {

                if (message.equals("В черном списке")) {

                    cv.put("verifyOrder", "0");
                    database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                }
            }
            database.close();
        }
    }
}
