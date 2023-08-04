package com.taxi.easy.ua.ui.start;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.R;

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
                startActivity(new Intent(StopActivity.this, StartActivity.class));
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
                Intent intent = new Intent(StopActivity.this, StartActivity.class);
                startActivity(intent);
            }
        });
    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onResume() {
        super.onResume();
    }

}
