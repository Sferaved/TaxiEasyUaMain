package com.taxi.easy.ua.ui.start;

import static com.taxi.easy.ua.ui.start.StartActivity.READ_CALL_PHONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class GoogleSignInActivity extends Activity {
    private static final int RC_SIGN_IN = 9001;
    private final String TAG = "TAG";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
//            return;
        }

        setContentView(R.layout.start_layout);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);

        }

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:0674443804"));
                if (ActivityCompat.checkSelfPermission(GoogleSignInActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);
                } else
                    startActivity(intent);
            }
        });


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

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    @Override

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode" + requestCode);
        Log.d(TAG, "onActivityResult: resultCode " + resultCode);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                handleSignInResult(task);
            } catch (MalformedURLException | JSONException | InterruptedException e) {
                throw new RuntimeException(e);
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
    public void updateUI(GoogleSignInAccount account) throws MalformedURLException, JSONException, InterruptedException {

        if(account != null){
            StartActivity.userEmail = account.getEmail();
            StartActivity.displayName = account.getDisplayName();

            addUser();

            String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/verifyBlackListUser/" +  StartActivity.userEmail;

            Log.d("TAG", "onClick urlCost: " + url);
            Map sendUrlMap = CostJSONParser.sendURL(url);

            String message = (String) sendUrlMap.get("message");
            Log.d("TAG", "onClick orderCost : " + message);

            if (message.equals("Не черном списке")) {

                Log.d("TAG", "onSignInResult: " + account.getEmail() + " " + account.getDisplayName());
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(GoogleSignInActivity.this, MainActivity.class);
                    startActivity(intent);
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(GoogleSignInActivity.this, OpenStreetMapActivity.class);
                    startActivity(intent);
                }
                MainActivity.verifyOrder = true;
            }
            if (message.equals("В черном списке")) {


                Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_LONG).show();


            }







        }else {
            Toast.makeText(this,"You Didnt signed in",Toast.LENGTH_LONG).show();
        }

    }

    private void addUser() throws MalformedURLException {
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

    @Override
    protected void onRestart() {
        super.onRestart();
        Button btn_again = findViewById(R.id.btn_again);
        btn_again.setVisibility(View.VISIBLE);
        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GoogleSignInActivity.this, StartActivity.class);
                startActivity(intent);
            }
        });
    }
}
