package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;

public class GetStartedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        ImageView ivBackground = findViewById(R.id.ivBackground);
        Button btnGetStarted = findViewById(R.id.btnGetStarted);

        // Transition to LoginActivity on button click
        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(GetStartedActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}
