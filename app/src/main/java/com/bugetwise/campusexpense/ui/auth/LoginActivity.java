package com.bugetwise.campusexpense.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bugetwise.campusexpense.MainActivity;
import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.database.AppDatabase;
import com.bugetwise.campusexpense.data.database.UserDao;
import com.bugetwise.campusexpense.data.model.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.security.MessageDigest;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private Button LoginButton;
    private UserDao userDao;
    private SharedPreferences sharedPreferences;

    private TextView registerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        registerText = findViewById(R.id.registerText);
        registerText.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        LoginButton = findViewById(R.id.loginButton);
        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        LoginButton.setOnClickListener(v -> login());
        if (isLoggedIn()) {
            goToMainActivity();
            return;
        }
    }

    private boolean isLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);

    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }

    private void login() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        usernameLayout.setError(null);
        passwordLayout.setError(null);
        if (username.isEmpty()) {
            usernameLayout.setError(getString(R.string.error_empty_username));
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_empty_password));
            return;
        }
        LoginButton.setEnabled(false);
        LoginButton.setText("Logging in...");

        new Thread(() -> {
            String hashedPassword = hashPassword(password);
            User user = userDao.login(username, hashedPassword);

            runOnUiThread(() -> {
                LoginButton.setEnabled(true);
                LoginButton.setText(R.string.login);
                if (user != null) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putInt("userId", user.getId());
                    editor.putString("username", user.getUsername());
                    editor.apply();
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                } else {
                    passwordLayout.setError(getString(R.string.error_invalid_credentials));
                }
            });
        }).start();
    }
}
