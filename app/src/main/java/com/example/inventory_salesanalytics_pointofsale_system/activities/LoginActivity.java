package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventory_salesanalytics_pointofsale_system.MainActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ImageView ivBackground;
    private EditText etUsername;
    private TextInputLayout tilPassword;
    private EditText etPassword;
    private TextView tvForgot, tvBottom;
    private Button btnLogin;
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupSpannableText();

        resetViewsToVisible();
        startEntranceAnimations();

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> handleLogin());
        }

        if (tvForgot != null) {
            tvForgot.setOnClickListener(v -> showForgotPasswordDialog());
        }

        if (tvBottom != null) {
            tvBottom.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            });
        }
    }

    private void showForgotPasswordDialog() {
        final EditText input = new EditText(this);
        input.setHint("Email address");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 10, padding, 10);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to recover your account.")
                .setView(container)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (dbHelper.checkEmail(email)) {
                        showLocalPasswordResetDialog(email);
                    } else {
                        sendPasswordResetEmail(email);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLocalPasswordResetDialog(String email) {
        final EditText input = new EditText(this);
        input.setHint("New Password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 10, padding, 10);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Local Password")
                .setMessage("Email verified. Enter your new password:")
                .setView(container)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = input.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    } else {
                        if (dbHelper.updatePasswordByEmail(email, newPass)) {
                            showSuccessDialog("Password updated successfully!");
                        } else {
                            showErrorDialog("Update failed. Please try again.");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showSuccessDialog("Password reset email sent (Firebase)");
                    } else {
                        showErrorDialog(task.getException() != null ? task.getException().getMessage() : "User not found");
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        } else if (getSharedPreferences("LoginPrefs", MODE_PRIVATE).getBoolean("isLoggedIn", false)) {
            navigateToMain();
        }
    }

    private void initViews() {
        ivBackground = findViewById(R.id.ivBackground);
        etUsername = findViewById(R.id.etUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        tvForgot = findViewById(R.id.tvForgot);
        btnLogin = findViewById(R.id.btnEmail);
        tvBottom = findViewById(R.id.tvBottom);
    }

    private void resetViewsToVisible() {
        if (etUsername != null) {
            etUsername.setAlpha(1f);
            etUsername.setTranslationY(0f);
        }
        if (tilPassword != null) {
            tilPassword.setAlpha(1f);
            tilPassword.setTranslationY(0f);
        }
        if (btnLogin != null) {
            btnLogin.setAlpha(1f);
            btnLogin.setTranslationY(0f);
        }
        if (tvBottom != null) {
            tvBottom.setAlpha(1f);
            tvBottom.setTranslationY(0f);
        }
        if (tvForgot != null) {
            tvForgot.setAlpha(1f);
            tvForgot.setTranslationY(0f);
        }
    }

    private void handleLogin() {
        if (etUsername == null || etPassword == null) {
            Toast.makeText(this, "Login fields are missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String inputUsername = etUsername.getText().toString().trim();
        String inputPass = etPassword.getText().toString().trim();

        if (inputUsername.isEmpty()) {
            etUsername.setError("Please enter your username");
            etUsername.requestFocus();
            return;
        }

        if (inputPass.isEmpty()) {
            if (tilPassword != null) tilPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        } else {
            if (tilPassword != null) tilPassword.setError(null);
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging In...");

        // 1. Try Local SQLite Database
        if (dbHelper.validateAdmin(inputUsername, inputPass)) {
            String email = dbHelper.getEmailByUsername(inputUsername);
            saveLoginStatus(email != null ? email : inputUsername);
            navigateToMain();
            return;
        }

        // 2. Try Firebase Authentication (Requires Email)
        String associatedEmail = dbHelper.getEmailByUsername(inputUsername);
        if (associatedEmail != null) {
            mAuth.signInWithEmailAndPassword(associatedEmail, inputPass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            saveLoginStatus(associatedEmail);
                            navigateToMain();
                        } else {
                            handleLoginError(task.getException());
                        }
                    });
        } else {
            // Fallback: If username not in local DB, maybe it was an email entered?
            mAuth.signInWithEmailAndPassword(inputUsername, inputPass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveLoginStatus(inputUsername);
                        navigateToMain();
                    } else {
                        handleLoginError(task.getException());
                    }
                });
        }
    }

    private void handleLoginError(Exception exception) {
        resetLoginButton();
        String errorMsg = "Invalid username or password. Please try again.";
        
        if (exception instanceof FirebaseNetworkException) {
            errorMsg = "No internet connection. Please check your network.";
        } else if (exception != null && exception.getMessage() != null) {
            if (exception.getMessage().toLowerCase().contains("configuration not found")) {
                errorMsg = "Service unavailable. Please contact the administrator.";
            }
        }
        showErrorDialog(errorMsg);
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Login Failed")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSuccessDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void saveLoginStatus(String email) {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("loggedEmail", email);
        editor.apply();
    }

    private void resetLoginButton() {
        if (btnLogin != null) {
            btnLogin.setEnabled(true);
            btnLogin.setText("LOGIN");
        }
    }

    private void navigateToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void setupSpannableText() {
        if (tvBottom == null) return;
        String text = "Don't have an account? Sign up";
        SpannableString ss = new SpannableString(text);
        int startIndex = text.indexOf("Sign up");
        if (startIndex != -1) {
            ss.setSpan(new ForegroundColorSpan(0xFFD4A056), startIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvBottom.setText(ss);
    }

    private void startEntranceAnimations() {
        if (ivBackground != null) ivBackground.animate().alpha(1f).setDuration(1000).start();
        Handler handler = new Handler();
        handler.postDelayed(() -> animateUp(etUsername), 200);
        handler.postDelayed(() -> animateUp(tilPassword), 350);
        handler.postDelayed(() -> animateUp(tvForgot), 500);
        handler.postDelayed(() -> {
            if (btnLogin != null) {
                btnLogin.setTranslationY(100f);
                btnLogin.setScaleX(0.8f);
                btnLogin.setScaleY(0.8f);
                btnLogin.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(700)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }, 700);
        handler.postDelayed(() -> animateUp(tvBottom), 1050);
    }

    private void animateUp(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(60f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
