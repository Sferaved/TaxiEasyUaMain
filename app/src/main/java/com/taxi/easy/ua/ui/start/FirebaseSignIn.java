package com.taxi.easy.ua.ui.start;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NotificationHelper;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class FirebaseSignIn extends AppCompatActivity {

    static FloatingActionButton fab, btn_again;
    public static final int READ_CALL_PHONE = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
        }
        ImageView mImageView = findViewById(R.id.imageView2);
        Animation sunRiseAnimation = AnimationUtils.loadAnimation(this, R.anim.sun_rise);
        // Подключаем анимацию к нужному View
        mImageView.startAnimation(sunRiseAnimation);


        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:0674443804"));
                startActivity(intent);
            }
        });


        FirebaseApp.initializeApp(FirebaseSignIn.this);

// Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build());

// Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        try {
            signInLauncher.launch(signInIntent);
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        btn_again = findViewById(R.id.btn_again);
        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirebaseSignIn.this, StartActivity.class));
            }
        });
    }

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    try {
                        onSignInResult(result);
                    } catch (MalformedURLException | JSONException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
    );

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) throws MalformedURLException, JSONException, InterruptedException {


        try {
            if (result.getResultCode() == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                StartActivity.userEmail = user.getEmail();
                StartActivity.displayName = user.getDisplayName();

                addUser();
                if (blackList()) {
                    checkLocationServiceEnabled(new LocationServiceCallback() {
                        @Override
                        public void onLocationServiceResult(boolean isEnabled) throws MalformedURLException {
                            // Обработайте результат isEnabled здесь
                            if (isEnabled) {
                                version();
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    Intent intent = new Intent(FirebaseSignIn.this, MainActivity.class);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(FirebaseSignIn.this, OpenStreetMapActivity.class);
                                    startActivity(intent);
                                }
                            } else {
                                Intent intent = new Intent(FirebaseSignIn.this, MainActivity.class);
                                startActivity(intent);
                            }
                        }
                    });



                    ContentValues cv = new ContentValues();

                    cv.put("verifyOrder", "1");
                    SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
                    // обновляем по id
                    database.update(StartActivity.TABLE_USER_INFO, cv, "id = ?",
                            new String[] { "1" });
                    database.close();

                } else {
                    Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
                }
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
                btn_again.setVisibility(View.VISIBLE);
                ContentValues cv = new ContentValues();

                cv.put("verifyOrder", "0");
                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
                // обновляем по id
                database.update(StartActivity.TABLE_USER_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();

            }
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
            ContentValues cv = new ContentValues();

            cv.put("verifyOrder", "0");
            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
            // обновляем по id
            database.update(StartActivity.TABLE_USER_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
        }
    }

    private void checkLocationServiceEnabled(LocationServiceCallback callback) throws MalformedURLException {
        Context context = getApplicationContext(); // Получите контекст вашего приложения

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // Проверяем, доступны ли данные о местоположении
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationServiceResult(false);
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                try {
                    callback.onLocationServiceResult(true);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    callback.onLocationServiceResult(false);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    // Интерфейс колбэка
    public interface LocationServiceCallback {
        void onLocationServiceResult(boolean isEnabled) throws MalformedURLException;
    }
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }
    // This function is called when user accept or decline the permission.
// Request Code is used to check which permission called this function.
// This request code is provided when user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
    private void addUser() throws MalformedURLException, JSONException, InterruptedException {
        String urlString = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/addUser/" +  StartActivity.displayName + "/" + StartActivity.userEmail;

        URL url = new URL(urlString);
        Log.d("TAG", "sendURL: " + urlString);

        AsyncTask.execute(() -> {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.getResponseCode();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            urlConnection.disconnect();
        });



    }

    private boolean blackList() throws MalformedURLException, JSONException, InterruptedException {
        String urlString = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/verifyBlackListUser/" +  StartActivity.userEmail;

        Log.d("TAG", "onClick urlCost: " + urlString);

        boolean result = false;
        Map sendUrlMap = CostJSONParser.sendURL(urlString);

        String message = (String) sendUrlMap.get("message");
        Log.d("TAG", "onClick orderCost : " + message);

        if (message.equals("Не черном списке")) {
            result = true;
        }
        if (message.equals("В черном списке")) {
            result = false;
        }
        return result;
    }

    private void version() throws MalformedURLException {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.POST_NOTIFICATIONS, PackageManager.PERMISSION_GRANTED);
            return;
        }

        String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/" +"versionAPI";


        Log.d("TAG", "onClick urlCost: " + url);
        Map sendUrlMapCost = null;
        try {
            sendUrlMapCost = ResultSONParser.sendURL(url);
        } catch (MalformedURLException | InterruptedException | JSONException e) {
            throw new RuntimeException(e);
        }

        String message = (String) sendUrlMapCost.get("message");
        if(!message.equals(getString(R.string.version_code))) {
            NotificationHelper notificationHelper = new NotificationHelper();

            String title = getString(R.string.new_version);
            String messageNotif = getString(R.string.news_of_version);
            String urlStr = "https://play.google.com/store/apps/details?id=com.taxi.easy.ua&pli=1";

            notificationHelper.showNotification(this, title, messageNotif, urlStr);
        }



    }
}