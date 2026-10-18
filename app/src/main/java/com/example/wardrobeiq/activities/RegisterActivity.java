package com.example.wardrobeiq.activities;

import android.content.Intent;
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

public class RegisterActivity extends AppCompatActivity {

    EditText editName, editEmail, editPassword;
    Button btnRegister;
    TextView txtLogin;

    UserDao userDao;

    ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);

        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);

        AppDatabase database =
                AppDatabase.getInstance(getApplicationContext());

        userDao = database.userDao();

        btnRegister.setOnClickListener(v -> registerUser());

        txtLogin.setOnClickListener(v -> {

            Intent intent =
                    new Intent(RegisterActivity.this,
                            LoginActivity.class);

            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {

        String name =
                editName.getText().toString().trim();

        String email =
                editEmail.getText().toString().trim().toLowerCase();

        String password =
                editPassword.getText().toString().trim();

        // Name validation
        if (name.isEmpty()) {
            editName.setError("Please enter your name");
            editName.requestFocus();
            return;
        }

        // Email validation
        if (email.isEmpty()) {
            editEmail.setError("Please enter your email");
            editEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            editEmail.setError("Please enter a valid email");
            editEmail.requestFocus();
            return;
        }

        // Password validation
        if (password.isEmpty()) {
            editPassword.setError("Please enter a password");
            editPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editPassword.setError(
                    "Password must contain at least 6 characters"
            );
            editPassword.requestFocus();
            return;
        }

        // Database operation
        executorService.execute(() -> {

            User existingUser =
                    userDao.getUserByEmail(email);

            if (existingUser != null) {

                runOnUiThread(() -> {

                    editEmail.setError(
                            "An account with this email already exists"
                    );

                    editEmail.requestFocus();

                });

                return;
            }

            String passwordHash =
                    PasswordUtils.hashPassword(password);

            User newUser =
                    new User(
                            name,
                            email,
                            passwordHash
                    );

            userDao.insertUser(newUser);

            runOnUiThread(() -> {

                Toast.makeText(
                        RegisterActivity.this,
                        "Registration successful",
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent =
                        new Intent(
                                RegisterActivity.this,
                                LoginActivity.class
                        );

                startActivity(intent);
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}