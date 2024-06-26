package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.fondy.token_pay.ApiResponseToken;
import com.taxi.easy.ua.ui.fondy.token_pay.PaymentApiToken;
import com.taxi.easy.ua.ui.fondy.token_pay.RequestDataToken;
import com.taxi.easy.ua.ui.fondy.token_pay.StatusRequestToken;
import com.taxi.easy.ua.ui.fondy.token_pay.SuccessResponseDataToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetTokenFragment extends BottomSheetDialogFragment {

    private static final String TAG = "TAG4";
    ListView listView;
    String tableToken;
    private AppCompatButton btn_ok;
    private int selectedPosition = -1;

    public MyBottomSheetTokenFragment(String tableToken) {
        this.tableToken = tableToken;
    }

    @SuppressLint("MissingInflatedId")

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bonus_list_layout, container, false);

        listView = view.findViewById(R.id.listViewBonus);

        ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase();
        Log.d(TAG, "onResume: cardMaps" + cardMaps);
        if (!cardMaps.isEmpty()) {
            CustomCardAdapterToken listAdapter = new CustomCardAdapterToken(requireActivity(), cardMaps);
            listView.setAdapter(listAdapter);


        }
        btn_ok =  view.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentByTokenFondy(MainActivity.order_id, FinishActivity.messageFondy, FinishActivity.amount, CustomCardAdapterToken.rectoken);
                dismiss();
            }
        });
        return view;
    }

    private void paymentByTokenFondy(
            String order_id,
            String orderDescription,
            String amount,
            String rectoken
    ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PaymentApiToken paymentApi = retrofit.create(PaymentApiToken.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, requireActivity());
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);
        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO, requireActivity());
        String email = stringList.get(3);

        RequestDataToken paymentRequest = new RequestDataToken(
                order_id,
                orderDescription,
                amount,
                MERCHANT_ID,
                merchantPassword,
                rectoken,
                email
        );


        StatusRequestToken statusRequest = new StatusRequestToken(paymentRequest);
        Log.d("TAG1", "getUrlToPayment: " + statusRequest);

        Call<ApiResponseToken<SuccessResponseDataToken>> call = paymentApi.makePayment(statusRequest);

        call.enqueue(new Callback<ApiResponseToken<SuccessResponseDataToken>>() {

            @Override
            public void onResponse(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, Response<ApiResponseToken<SuccessResponseDataToken>> response) {
                Log.d("TAG1", "onResponse: 1111" + response.code());
                if (response.isSuccessful()) {
                    ApiResponseToken<SuccessResponseDataToken> apiResponse = response.body();

                    Log.d("TAG1", "onResponse: " +  new Gson().toJson(apiResponse));
                    try {
                        SuccessResponseDataToken responseBody = response.body().getResponse();;

                        // Теперь у вас есть объект ResponseBodyRev для обработки
                        if (responseBody != null) {
                            Log.d(TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                            String orderStatus = responseBody.getOrderStatus();
                            if (!"approved".equals(orderStatus)) {
                                // Обработка ответа об ошибке
                                String errorResponseMessage = responseBody.getErrorMessage();
                                String errorResponseCode = responseBody.getErrorCode();
                                Log.d("TAG1", "onResponse: errorResponseMessage " + errorResponseMessage);
                                Log.d("TAG1", "onResponse: errorResponseCode" + errorResponseCode);

                                cancelOrderDismiss(FinishActivity.uid);
                                cancelOrderDismiss(FinishActivity.uid_Double);
                            }
                        } else {
                            cancelOrderDismiss(FinishActivity.uid);
                            cancelOrderDismiss(FinishActivity.uid_Double);
                        }
                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        FirebaseCrashlytics.getInstance().recordException(e);
                        Log.e("TAG1", "Error parsing JSON response: " + e.getMessage());
                        cancelOrderDismiss(FinishActivity.uid);
                        cancelOrderDismiss(FinishActivity.uid_Double);
                    }
                } else {
                    // Обработка ошибки
                    Log.d("TAG1", "onFailure: " + response.code());
                    cancelOrderDismiss(FinishActivity.uid);
                    cancelOrderDismiss(FinishActivity.uid_Double);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, @NonNull Throwable t) {
                Log.d("TAG1", "onFailure1111: " + t.toString());

                cancelOrderDismiss(FinishActivity.uid);
                cancelOrderDismiss(FinishActivity.uid_Double);
            }
        });
    }

    private void cancelOrderDismiss(String value) {
        List<String> listCity = logCursor(MainActivity.CITY_INFO, requireActivity());
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = FinishActivity.baseUrl + "/" + api + "/android/webordersCancel/" + value + "/" + city  + "/" + requireActivity().getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Log.d("TAG", "cancelOrderWithDifferentValue cancelOrderUrl: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                Status status = response.body();
                if (status != null) {

                    String result =  String.valueOf(status.getResponse());
                    Log.d("TAG", "onResponse: result" + result);
                    FinishActivity.text_status.setText(result + getString(R.string.pay_failure));

                } else {
                    FinishActivity.text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                FirebaseCrashlytics.getInstance().recordException(t);
                Log.d("TAG", "onFailure: " + errorMessage);

            }
        });
    }

    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase() {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        Cursor cursor = database.query(tableToken, null, null, null, null, null, null);
        Log.d(TAG, "getCardMapsFromDatabase: tableToken card count: " + cursor.getCount());

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Map<String, String> cardMap = new HashMap<>();
                    cardMap.put("card_type", cursor.getString(cursor.getColumnIndex("card_type")));
                    cardMap.put("bank_name", cursor.getString(cursor.getColumnIndex("bank_name")));
                    cardMap.put("masked_card", cursor.getString(cursor.getColumnIndex("masked_card")));
                    cardMap.put("rectoken", cursor.getString(cursor.getColumnIndex("rectoken")));

                    cardMaps.add(cardMap);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        database.close();

        return cardMaps;
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
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

