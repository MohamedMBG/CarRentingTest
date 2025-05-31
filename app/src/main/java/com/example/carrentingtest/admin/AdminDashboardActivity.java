package com.example.carrentingtest.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.carrentingtest.R;
import com.example.carrentingtest.SignInActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private TextView tvPendingCount, tvAvailableCount, tvTotalCars;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Consider removing if using CoordinatorLayout/Toolbar
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Toolbar (if using the new layout)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Optional: Add navigation icon click listener if needed
        // toolbar.setNavigationOnClickListener(v -> { /* Handle navigation */ });

        // Initialize TextViews for stats
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvAvailableCount = findViewById(R.id.tvAvailableCount);
        tvTotalCars = findViewById(R.id.tvTotalCars);

        // Setup click listeners for cards
        findViewById(R.id.cardManageCars).setOnClickListener(v ->
                startActivity(new Intent(this, ManageCarsActivity.class)));

        findViewById(R.id.cardViewRequests).setOnClickListener(v ->
                startActivity(new Intent(this, ViewRequestsActivity.class)));

        // Setup logout button click listener
        findViewById(R.id.btnLogout).setOnClickListener(v -> logoutAdmin());

        // Fetch and display stats
        fetchDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats when the activity resumes, in case data changed
        fetchDashboardStats();
    }

    private void fetchDashboardStats() {
        // Fetch Pending Requests Count
        db.collection("rental_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int pendingCount = queryDocumentSnapshots.size();
                    tvPendingCount.setText(String.valueOf(pendingCount));
                    Log.d(TAG, "Pending requests count: " + pendingCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching pending requests count", e);
                    tvPendingCount.setText("?"); // Indicate error
                });

        // Fetch Available Cars Count
        db.collection("cars")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int availableCount = queryDocumentSnapshots.size();
                    tvAvailableCount.setText(String.valueOf(availableCount));
                    Log.d(TAG, "Available cars count: " + availableCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching available cars count", e);
                    tvAvailableCount.setText("?"); // Indicate error
                });

        // Fetch Total Cars Count
        db.collection("cars")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalCount = queryDocumentSnapshots.size();
                    tvTotalCars.setText(String.valueOf(totalCount));
                    Log.d(TAG, "Total cars count: " + totalCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching total cars count", e);
                    tvTotalCars.setText("?"); // Indicate error
                });
    }

    private void logoutAdmin() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SignInActivity.class); // Redirect to main sign-in
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
