package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);

        if (btnSignUp != null) {
            btnSignUp.setText("CREATE ACCOUNT");
            btnSignUp.setOnClickListener(v -> handleSignUp());
        }
        
        setupSpannableText();
        tvLogin.setOnClickListener(v -> finish());
    }

    private void setupSpannableText() {
        String text = "Already have an account? Log In";
        SpannableString ss = new SpannableString(text);
        int startIndex = text.indexOf("Log In");
        if (startIndex != -1) {
            ss.setSpan(new ForegroundColorSpan(0xFFD4A056), startIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvLogin.setText(ss);
    }

    private void handleSignUp() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password should be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating Account...");

        // 1. Save to Local Database FIRST (Offline fallback)
        boolean savedLocally = saveUserToLocalDatabase(username, email, password);

        // 2. Attempt Firebase Sync
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();
                            user.updateProfile(profileUpdates);
                        }
                        
                        Toast.makeText(SignUpActivity.this, "Account Created Successfully! Please login.", Toast.LENGTH_LONG).show();
                        finish(); 
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                        
                        if (errorMsg.contains("CONFIGURATION_NOT_FOUND") || errorMsg.contains("internal error")) {
                            if (savedLocally) {
                                Toast.makeText(SignUpActivity.this, "Account Created Locally! Please login.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                resetSignUpButton("Error creating account.");
                            }
                        } else {
                            resetSignUpButton(errorMsg);
                        }
                    }
                });
    }

    private void resetSignUpButton(String error) {
        btnSignUp.setEnabled(true);
        btnSignUp.setText("CREATE ACCOUNT");
        Toast.makeText(SignUpActivity.this, error, Toast.LENGTH_LONG).show();
    }

    private boolean saveUserToLocalDatabase(String username, String email, String password) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.KEY_ADMIN_USERNAME, username);
            values.put(DatabaseHelper.KEY_ADMIN_PASSWORD, password);
            values.put(DatabaseHelper.KEY_ADMIN_EMAIL, email);
            long id = db.insert(DatabaseHelper.TABLE_ADMIN, null, values);
            return id != -1;
        } catch (Exception e) {
            return false;
        }
    }
}
