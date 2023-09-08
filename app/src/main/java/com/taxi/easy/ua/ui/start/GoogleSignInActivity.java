package com.taxi.easy.ua.ui.start;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

public class GoogleSignInActivity extends Activity {
    private static final int RC_SIGN_IN = 9001;
    private final String TAG = "TAG";
    static FloatingActionButton fab, btn_again;
    Button try_again_button;
    String api;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

// Создание клиента авторизации
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

// Запуск активности авторизации
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @SuppressLint("Range")
    public List<String> logCursor(String table) {
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
    @Override

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                handleSignInResult(task);
            } catch (MalformedURLException | JSONException | InterruptedException e) {
                Log.d(TAG, "onActivityResult: " + new RuntimeException(e));
            }
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) throws MalformedURLException, JSONException, InterruptedException {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    //Change UI according to user data.
    public void updateUI(GoogleSignInAccount account) throws MalformedURLException {
        SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();
        if(account != null){

            updateRecordsUserInfo("email", account.getEmail());
            updateRecordsUserInfo("username", account.getDisplayName());

            addUser(account.getEmail(), account.getDisplayName());

            cv.put("verifyOrder", "1");
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            requestLocationPermissions();
            finish();
            startActivity(new Intent(GoogleSignInActivity.this, MainActivity.class));
        }else {
            Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
            try_again_button.setVisibility(View.VISIBLE);
            cv.put("verifyOrder", "0");
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
        }
        database.close();

    }
    private void updateRecordsUserInfo(String userInfo, String result) {
        SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();

        cv.put(userInfo, result);

        // обновляем по id
        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    private void addUser(String displayName, String userEmail) {
        String urlString = "https://m.easy-order-taxi.site/" + MainActivity.apiKyiv + "/android/addUser/" + displayName + "/" + userEmail;

        Callable<Void> addUserCallable = () -> {
            URL url = new URL(urlString);
            Log.d("TAG", "sendURL: " + urlString);

            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
//                urlConnection.getResponseCode();
                Log.d(TAG, "addUser: urlConnection.getResponseCode(); " + urlConnection.getResponseCode());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        };

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Void> addUserFuture = executorService.submit(addUserCallable);

    // Дождитесь завершения выполнения задачи с тайм-аутом
    try {
        addUserFuture.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
        // Обработка ошибок
        e.printStackTrace();
    } finally {
        // Завершите исполнителя
        executorService.shutdown();
    }
}
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001; // Произвольный код для запроса разрешений


    private void requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Показать объяснение пользователю почему нужны разрешения (если необходимо)

            // Затем запросить разрешения
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Запросить разрешения
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        try_again_button.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SuspiciousIndentation")
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

        try_again_button = findViewById(R.id.try_again_button);
        try_again_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

    }
}
