package com.taxi.easy.ua.ui.finish;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import  com.taxi.easy.ua.MainActivity;
import  com.taxi.easy.ua.R;
import  com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import  com.taxi.easy.ua.ui.start.StartActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FinishActivity extends AppCompatActivity {
    private TextView text_full_message, text_status;
    private Button btn_reset_status, btn_cancel_order, btn_again, btn_cancel;
    private FloatingActionButton fab_cal;
    String api;
    String baseUrl = "https://m.easy-order-taxi.site";
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);

        List<String> stringListArr = logCursor(StartActivity.CITY_INFO);
        switch (stringListArr.get(1)){
            case "Kyiv City":
                api = StartActivity.apiKyiv;
                break;
            case "Odessa":
                api = StartActivity.apiTest;
                break;
            default:
                api = StartActivity.apiTest;
                break;
        }
        String parameterValue = getIntent().getStringExtra("messageResult_key");

        text_full_message = findViewById(R.id.text_full_message);
        text_full_message.setText(parameterValue);

        String UID_key = getIntent().getStringExtra("UID_key");

        text_status = findViewById(R.id.text_status);
        statusOrderWithDifferentValue(UID_key);


        btn_reset_status = findViewById(R.id.btn_reset_status);
        btn_reset_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    statusOrderWithDifferentValue(UID_key);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_cancel_order = findViewById(R.id.btn_cancel_order);
        btn_cancel_order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    cancelOrderWithDifferentValue(UID_key);
                } else {
//                    String result = getString(R.string.next_try);
                    String result = "111111111111";
                    text_status.setText(result);
                }
            }
        });

        btn_again = findViewById(R.id.btn_again);
        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), OpenStreetMapActivity.class);
                        startActivity(intent);
                    }
                } else {
                    {
                        Toast.makeText(getApplicationContext(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });
        fab_cal = findViewById(R.id.fab_call);
        fab_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:0674443804"));
                startActivity(intent);
            }
        });
    }
    private boolean connected() {

        Boolean hasConnect = false;

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

        if (!hasConnect) {
            Toast.makeText(this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
        }
        return hasConnect;
    }
    @SuppressLint("Range")
    private List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = this.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
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
                        String result = status.getResponse();
                        text_status.setText(result); // Установите текстовое значение в text_status
                    }
                } else {
                    // Обработка неуспешного ответа
                    text_status.setText("Ошибка запроса к серверу");
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d("TAG", "onFailure: " + errorMessage);
                text_status.setText("Ошибка запроса к серверу");
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
                            message = getString(R.string.ex_st_0);
                            break;
                    }

                    text_status.setText(message);

                } else {
                    // Обработка ошибки, если запрос был выполнен не успешно
                    // например:
                    String errorBody = response.errorBody().toString();
                    // Обрабатываем ошибку в зависимости от вашего случая
                    text_status.setText(errorBody);
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                // Обработка ошибки, если запрос не удался
                // например:
                String errorMessage = t.getMessage();
                // Обрабатываем ошибку в зависимости от вашего случая
                text_status.setText(errorMessage);
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
            throw new RuntimeException(e);
        }

        // Форматируем дату и время в украинском формате
        return outputFormat.format(date);

    }
}
