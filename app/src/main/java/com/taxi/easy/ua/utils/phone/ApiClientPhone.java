package com.taxi.easy.ua.utils.phone;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientPhone {

//    private static final String BASE_URL = "https://m.easy-order-taxi.site/";
    private static final String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

    private final ApiServicePhone apiService;

    public ApiClientPhone() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiServicePhone.class);
    }

    public void getUserPhone(String email, final OnUserPhoneResponseListener listener) {
        Call<UserPhoneResponse> call = apiService.getUserPhone(email);
        call.enqueue(new Callback<UserPhoneResponse>() {
            @Override
            public void onResponse(Call<UserPhoneResponse> call, Response<UserPhoneResponse> response) {
                if (response.isSuccessful()) {
                    UserPhoneResponse userPhoneResponse = response.body();
                    if (userPhoneResponse != null) {
                        listener.onSuccess(userPhoneResponse.getPhone());
                    } else {
                        listener.onError("Response body is null");
                    }
                } else {
                    listener.onError("Request not successful: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<UserPhoneResponse> call, Throwable t) {
                listener.onError("Request failed: " + t.getMessage());
            }
        });
    }

    public interface OnUserPhoneResponseListener {
        void onSuccess(String phone);

        void onError(String error);
    }
}
