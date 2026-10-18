package com.example.wardrobeiq.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wardrobeiq.MainActivity;
import com.example.wardrobeiq.R;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.UserDao;
import com.example.wardrobeiq.models.User;
import com.example.wardrobeiq.utils.PasswordUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    EditText editUsername, editPassword;
    Button btnLogin;
    TextView txtRegister;

    UserDao userDao;

    ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences preferences =
                getSharedPreferences(
                        "WardrobeIQSession",
                        MODE_PRIVATE
                );

        boolean isLoggedIn =
                preferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            MainActivity.class
                    );

            startActivity(intent);
            finish();

            return;
        }

        setContentView(R.layout.activity_login);

        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);

        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        AppDatabase database =
                AppDatabase.getInstance(
                        getApplicationContext()
                );

        userDao = database.userDao();

        btnLogin.setOnClickListener(v -> loginUser());

        txtRegister.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            RegisterActivity.class
                    );

            startActivity(intent);
        });
    }

    private void loginUser() {

        String email =
                editUsername.getText()
                        .toString()
                        .trim()
                        .toLowerCase();

        String password =
                editPassword.getText()
                        .toString()
                        .trim();

        if (email.isEmpty()) {
            editUsername.setError(
                    "Please enter your email"
            );
            editUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editPassword.setError(
                    "Please enter your password"
            );
            editPassword.requestFocus();
            return;
        }

        executorService.execute(() -> {

            User user =
                    userDao.getUserByEmail(email);

            if (user == null) {

                runOnUiThread(() ->
                        Toast.makeText(
                                LoginActivity.this,
                                "Invalid email or password",
                                Toast.LENGTH_SHORT
                        ).show()
                );

                return;
            }

            String enteredPasswordHash =
                    PasswordUtils.hashPassword(password);

            if (enteredPasswordHash.equals(
                    user.getPasswordHash())) {

                // Save login session
                SharedPreferences session =
                        getSharedPreferences(
                                "WardrobeIQSession",
                                MODE_PRIVATE
                        );

                session.edit()
                        .putBoolean("isLoggedIn", true)
                        .putInt("userId", user.getId())
                        .putString("userName", user.getName())
                        .putString("userEmail", user.getEmail())
                        .apply();

                runOnUiThread(() -> {

                    Toast.makeText(
                            LoginActivity.this,
                            "Login successful",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent =
                            new Intent(
                                    LoginActivity.this,
                                    MainActivity.class
                            );

                    startActivity(intent);
                    finish();
                });

            } else {

                runOnUiThread(() ->
                        Toast.makeText(
                                LoginActivity.this,
                                "Invalid email or password",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}