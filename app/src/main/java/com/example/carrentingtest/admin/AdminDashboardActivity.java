package com.example.carrentingtest.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carrentingtest.R;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        findViewById(R.id.cardManageCars).setOnClickListener(v ->
                startActivity(new Intent(this, ManageCarsActivity.class)));

        findViewById(R.id.cardViewRequests).setOnClickListener(v ->
                startActivity(new Intent(this, ViewRequestsActivity.class)));

    }
}