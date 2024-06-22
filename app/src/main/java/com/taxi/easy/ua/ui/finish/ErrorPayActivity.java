package com.taxi.easy.ua.ui.finish;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.fondy.revers.ApiResponseRev;
import com.taxi.easy.ua.ui.fondy.revers.ReversApi;
import com.taxi.easy.ua.ui.fondy.revers.ReversRequestData;
import com.taxi.easy.ua.ui.fondy.revers.ReversRequestSent;
import com.taxi.easy.ua.ui.fondy.revers.SuccessResponseDataRevers;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetMessageFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ErrorPayActivity extends AppCompatActivity {

    private static final String TAG = "TAG1";
    private String messageError;
    private String urlOrder;
    private String orderCost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_pay);

        messageError = getIntent().getStringExtra("messageError");
        urlOrder = MainActivity.order_id;
        orderCost = getIntent().getStringExtra("orderCost") + "00";

        TextView textView = findViewById(R.id.textError);
        textView.setText(messageError);

        AppCompatButton button = findViewById(R.id.btnReturn);
        button.setOnClickListener(v -> getRevers(urlOrder, getString(R.string.return_pay), orderCost));

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        FloatingActionButton fab_call = findViewById(R.id.fab_call);
        fab_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                List<String> stringList = logCursor(MainActivity.CITY_INFO);
                String phone = stringList.get(3);
                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });
    }
    private void getRevers(String orderId, String comment, String amount) {
        Log.d(TAG, "getRevers: amount" + amount);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ReversApi apiService = retrofit.create(ReversApi.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);

        ReversRequestData reversRequestData = new ReversRequestData(
                orderId,
                comment,
                amount,
               MERCHANT_ID,
                merchantPassword
        );

        ReversRequestSent reversRequestSent = new ReversRequestSent(reversRequestData);

        Call<ApiResponseRev<SuccessResponseDataRevers>> call = apiService.makeRevers(reversRequestSent);

        call.enqueue(new Callback<ApiResponseRev<SuccessResponseDataRevers>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponseRev<SuccessResponseDataRevers>> call, @NonNull Response<ApiResponseRev<SuccessResponseDataRevers>> response) {

                if (response.isSuccessful()) {
                    // Обработка успешного ответа
                    ApiResponseRev<SuccessResponseDataRevers> apiResponse = response.body();
                    Log.d("TAG1", "JSON Response: " + new Gson().toJson(apiResponse));
                    try {
                        SuccessResponseDataRevers responseBody = response.body().getResponse();;

                        // Теперь у вас есть объект ResponseBodyRev для обработки
                        if (responseBody != null) {
                            String responseStatus = responseBody.getResponseStatus();
                            if ("success".equals(responseStatus)) {
                                // Обработка успешного ответа

                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(getString(R.string.check_pay_return));
                                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                            } else if ("failure".equals(responseStatus)) {
                                // Обработка ответа об ошибке
                                String errorResponseMessage = responseBody.getErrorMessage();
                                String errorResponseCode = responseBody.getErrorCode();

                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.return_pay_error));
                                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                                Log.d("TAG1", "onResponse: errorResponseMessage " + errorResponseMessage);
                                Log.d("TAG1", "onResponse: errorResponseCode" + errorResponseCode);
                                // Отобразить сообщение об ошибке пользователю
                            } else {
                                // Обработка других возможных статусов ответа
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.return_pay_error));
                                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                            }
                        } else {
                            // Обработка пустого тела ответа
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.return_pay_error));
                            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                        }
                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Log.e("TAG1", "Error parsing JSON response: " + e.getMessage());
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.return_pay_error));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }

                } else {
                    // Обработка ошибки запроса
                    Log.d("TAG", "onResponse: Ошибка запроса, код " + response.code());
                    assert response.errorBody() != null;
                    String errorBody = null;
                    try {
                        errorBody = response.errorBody().string();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.return_pay_error));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    } catch (IOException ignored) {

                    }


                }

            }

            @Override
            public void onFailure(Call<ApiResponseRev<SuccessResponseDataRevers>> call, Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d("TAG", "onFailure: Ошибка сети: " + t.getMessage());
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.return_pay_error));
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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