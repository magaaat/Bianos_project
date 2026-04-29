package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.google.android.material.button.MaterialButton;

public class NotificationDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        TextView tvTitle = findViewById(R.id.tvNotifTitle);
        TextView tvBody = findViewById(R.id.tvNotifBody);
        ImageView ivBack = findViewById(R.id.ivBack);
        MaterialButton btnDone = findViewById(R.id.btnDone);

        // Get data from intent
        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");

        tvTitle.setText(title != null ? title : "System Alert");
        tvBody.setText(message != null ? message : "No details available.");

        ivBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());
    }
}
