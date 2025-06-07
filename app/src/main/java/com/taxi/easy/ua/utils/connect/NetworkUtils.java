package com.taxi.easy.ua.utils.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.finish.Status;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkUtils {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        );
    }


    public interface ApiCallback {
        void onSuccess(boolean isStable);
        void onFailure(Throwable t);
    }

    public static void isInternetStable(ApiCallback callback) {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site/") // Укажите URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Status> call = apiService.check();

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull retrofit2.Response<Status> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i("isInternetStable", "Internet is stable.");
                    callback.onSuccess(true); // Передаем успех через колбэк
                } else {
                    Log.i("isInternetStable", "Internet is unstable.");
                    callback.onSuccess(false); // Указываем нестабильное подключение
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                Log.e("isInternetStable", "Error occurred during the request", t);
                callback.onFailure(t); // Передаем ошибку через колбэк
            }
        });
    }



}
