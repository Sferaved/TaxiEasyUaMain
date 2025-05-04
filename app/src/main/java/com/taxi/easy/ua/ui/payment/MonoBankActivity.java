package com.taxi.easy.ua.ui.payment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MonoBankActivity extends AppCompatActivity {
    private AppCompatButton btn;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mono_bank);
        btn = findViewById(R.id.pay_mono_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getClientInfo();
            }
        });
    }

    private void myMono() {
        String apiToken = getString(R.string.mono_key_storage);
        Log.d("TAG", "myMono: apiToken" + apiToken);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.monobank.ua/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MonoBankApiService monoBankApiService = retrofit.create(MonoBankApiService.class);

        String account = "0";
        String startDateString = "01/09/2023";
        String endDateString = "25/09/2023";

        // Определите формат даты
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        long startTimeInMillis = 0;
        long endTimeInMillis = 0;
        try {
            // Преобразуйте начальную и конечную даты из строк в объекты Date
            Date startDate = sdf.parse(startDateString);
            Date endDate = sdf.parse(endDateString);

            // Получите метки времени в миллисекундах
            startTimeInMillis = startDate.getTime();
            endTimeInMillis = endDate.getTime();

        } catch (ParseException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        Call<List<Transaction>> call = monoBankApiService.getTransactions(apiToken, account, String.valueOf(startTimeInMillis), String.valueOf(endTimeInMillis));
        call.enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> surfaceControlTransactions = response.body();
                    List<Transaction> transactions = new ArrayList<>();

                    for (Transaction surfaceTransaction : surfaceControlTransactions) {
                        Transaction transaction = new Transaction();
                        transaction.setId(surfaceTransaction.getId());
                        transaction.setTime(surfaceTransaction.getTime());
                        transaction.setDescription(surfaceTransaction.getDescription());
                        transaction.setMcc(surfaceTransaction.getMcc());
                        transaction.setOriginalMcc(surfaceTransaction.getOriginalMcc());
                        transaction.setHold(surfaceTransaction.isHold());
                        transaction.setAmount(surfaceTransaction.getAmount());
                        transaction.setOperationAmount(surfaceTransaction.getOperationAmount());
                        transaction.setCurrencyCode(surfaceTransaction.getCurrencyCode());
                        transaction.setCommissionRate(surfaceTransaction.getCommissionRate());
                        transaction.setCashbackAmount(surfaceTransaction.getCashbackAmount());
                        transaction.setBalance(surfaceTransaction.getBalance());
                        transaction.setComment(surfaceTransaction.getComment());
                        transaction.setReceiptId(surfaceTransaction.getReceiptId());
                        transaction.setInvoiceId(surfaceTransaction.getInvoiceId());
                        transaction.setCounterEdrpou(surfaceTransaction.getCounterEdrpou());
                        transaction.setCounterIban(surfaceTransaction.getCounterIban());
                        transaction.setCounterName(surfaceTransaction.getCounterName());

                        transactions.add(transaction);
                    }

                    // Теперь у вас есть список объектов Transaction для обработки
                    Log.d("TAG", "Transaction: " + transactions);
                } else {
                    // Обработка ошибки
                    Log.d("TAG", "response.errorBody(): " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                // Обработка ошибки
                Log.d("TAG", "response.body(): t " + t);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });


    }
    private void getClientInfo() {

        String apiToken = getString(R.string.mono_key_storage);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.monobank.ua/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MonoBankApiService monoBankApiService = retrofit.create(MonoBankApiService.class);

        Call<ClientInfo> call = monoBankApiService.getClientInfo(apiToken);
        call.enqueue(new Callback<ClientInfo>() {
            @Override
            public void onResponse(Call<ClientInfo> call, Response<ClientInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClientInfo clientInfo = response.body();
                    if (clientInfo != null) {
                        // Обработка данных о клиенте, счетах и копилках
                        Log.d("TAG", "ClientInfo: " + clientInfo);
                    }
                } else {
                    // Обработка ошибки
                    Log.d("TAG", "response.errorBody(): " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<ClientInfo> call, Throwable t) {
                // Обработка ошибки
                Log.d("TAG", "response.body(): t " + t);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

}