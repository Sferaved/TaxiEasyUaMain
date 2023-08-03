package com.taxi.easy.ua;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class ServerConnection {

    public interface ConnectionCallback {
        void onConnectionResult(boolean isConnected);
    }

    public static void checkConnection(String url, ConnectionCallback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean isConnected = response.isSuccessful();
                callback.onConnectionResult(isConnected);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onConnectionResult(false);
            }
        });
    }
}
