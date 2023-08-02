package com.taxi.easy.ua.ui.start;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import  com.taxi.easy.ua.R;

public class StopActivity extends Activity {
    static FloatingActionButton fab, btn_again;

    Button try_again_button;





    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
        fab = findViewById(R.id.fab);
        try_again_button = findViewById(R.id.try_again_button);
        try_again_button.setVisibility(View.VISIBLE);
        try_again_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasConnection()) {

                    Toast.makeText(StopActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(StopActivity.this, StartActivity.class);
                    startActivity(intent);
                }
            }
        });


        btn_again = findViewById(R.id.btn_again);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:0674443804"));
                startActivity(intent);
            }
        });


        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasConnection()) {

                    Toast.makeText(StopActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(StopActivity.this, StartActivity.class);
                    startActivity(intent);
                }
            }
        });




        Toast.makeText(this, R.string.slow_internet, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onResume() {
        super.onResume();







    }
    public boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) StopActivity.this.getSystemService(
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


}
