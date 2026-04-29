package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.inventory_salesanalytics_pointofsale_system.MainActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.ivSplashLogo);
        
        Glide.with(this)
                .load(R.mipmap.loadingscreen)
                .into(logo);

        new Handler().postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // User is signed in with Firebase, go to MainActivity
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else if (getSharedPreferences("LoginPrefs", MODE_PRIVATE).getBoolean("isLoggedIn", false)) {
                // User is signed in with Local account, go to MainActivity
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // No user signed in, go to GetStartedActivity
                startActivity(new Intent(SplashActivity.this, GetStartedActivity.class));
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2500);
    }
}
