package com.taxi.easy.ua.ui.start;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;


public class FirebaseSignIn extends AppCompatActivity {

    private static final String TAG = "FirebaseSignIn";
    FloatingActionButton fab, btn_again;
    Button try_again_button;

    private static final int REQUEST_ENABLE_GPS = 1001;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
       


        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone = "tel:0674443804";
                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });
        btn_again = findViewById(R.id.btn_again);

        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirebaseSignIn.this, MainActivity.class));
            }
        });
        try_again_button = findViewById(R.id.try_again_button);
        try_again_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirebaseSignIn.this, MainActivity.class));
            }
        });
        startSignInInBackground();
    }

    private void startSignInInBackground() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Инициализация FirebaseApp
                FirebaseApp.initializeApp(FirebaseSignIn.this);

                // Choose authentication providers
                List<AuthUI.IdpConfig> providers = Collections.singletonList(
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent
                Intent signInIntent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build();
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            signInLauncher.launch(signInIntent);
                        }
                    });
                } catch (NullPointerException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    finish();
                    startActivity(new Intent(FirebaseSignIn.this, StopActivity.class));
                }
            }
        });
        thread.start();
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
    protected void onResume() {
        super.onResume();

    }

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    try {
                        onSignInResult(result);
                    } catch (MalformedURLException | JSONException | InterruptedException e) {
                        Logger.d(getApplication(), TAG, "onCreate:" + new RuntimeException(e));
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }
            }
    );


    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) throws MalformedURLException, JSONException, InterruptedException {
        ContentValues cv = new ContentValues();
        try {
            if (result.getResultCode() == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                updateRecordsUserInfo("email", user.getEmail());
                updateRecordsUserInfo("username", user.getDisplayName());

                addUser(user.getEmail(), user.getDisplayName());

                // Проверяем состояние GPS


                // Здесь также происходит обновление значения verifyOrder в базе данных

                cv.put("verifyOrder", "1");
                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                database.close();

                Intent intent = new Intent(FirebaseSignIn.this, MainActivity.class);
                startActivity(intent);
            } else {
                // Sign in failed
                Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
                btn_again.setVisibility(View.VISIBLE);
                try_again_button.setVisibility(View.VISIBLE);
                cv.put("verifyOrder", "0");
                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                database.close();
            }
        } catch (NullPointerException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            // Error handling
            Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
            btn_again.setVisibility(View.VISIBLE);
            try_again_button.setVisibility(View.VISIBLE);
            cv.put("verifyOrder", "0");
            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
        }
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


// Другой код вашего Fragment или Activity...
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_GPS) {
            // Проверяем, был ли GPS успешно включен
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (isGPSEnabled) {
                // Если GPS включен после возвращения из окна настроек, запускаем OpenStreetMapActivity
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(this, OpenStreetMapActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
        }
    }

    private void addUser(String displayName, String userEmail) {
        String urlString = "https://m.easy-order-taxi.site/" + MainActivity.api + "/android/addUser/" + displayName + "/" + userEmail;

        Callable<Void> addUserCallable = () -> {
            URL url = new URL(urlString);
            Logger.d(getApplication(), TAG, "sendURL: " + urlString);

            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
//                urlConnection.getResponseCode();
                Logger.d(getApplication(), TAG, "addUser: urlConnection.getResponseCode(); " + urlConnection.getResponseCode());
            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
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
            addUserFuture.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Обработка ошибок
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            // Завершите исполнителя
            executorService.shutdown();
        }
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        Button try_again_button = findViewById(R.id.try_again_button);
        try_again_button.setVisibility(View.VISIBLE);
        try_again_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });
    }
}