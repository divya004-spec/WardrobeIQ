package com.example.wardrobeiq.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wardrobeiq.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            Intent intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);
            finish();

        }, SPLASH_DELAY);
    }
}