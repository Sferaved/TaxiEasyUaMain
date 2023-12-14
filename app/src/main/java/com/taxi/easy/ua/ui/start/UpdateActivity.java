package com.taxi.easy.ua.ui.start;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import java.util.ArrayList;
import java.util.List;

public class UpdateActivity extends Activity {
    static FloatingActionButton fab;
    Button btn_again;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
        Toast.makeText(this, R.string.update_message, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onResume() {
        super.onResume();

        fab = findViewById(R.id.fab);
        btn_again = findViewById(R.id.btn_again);
        btn_again.setVisibility(View.VISIBLE);
        btn_again.setText("GOOGLE PLAY MARKET");

        fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    String phone;
                    List<String> stringList = logCursor(MainActivity.CITY_INFO);
                    switch (stringList.get(1)){
                        case "message = getString(R.string.Dnipro_city);":
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


       btn_again.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               if(!hasConnection()) {
                   Toast.makeText(UpdateActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
               } else {
                   Intent intent = new Intent(Intent.ACTION_VIEW);
                   intent.setData(Uri.parse("https://play.google.com/store/apps/details?id= com.taxi.easy.ua"));
                   startActivity(intent);
               }
           }
       });




    }
    public boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) UpdateActivity.this.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            return true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            return true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true;
        }

        return false;
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table) {
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
        assert c != null;
        c.close();
        return list;
    }

}
