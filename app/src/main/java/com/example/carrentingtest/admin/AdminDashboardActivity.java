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

/**
 * Activity that displays the admin dashboard with key statistics
 * and provides navigation to admin management features.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    // UI Components to display statistics
    private TextView tvPending;    // Displays count of pending rental requests
    private TextView tvAvailable;  // Displays count of available cars
    private TextView tvTotal;      // Displays total count of all cars

    // Firebase services instances
    private FirebaseFirestore db = FirebaseFirestore.getInstance();  // Cloud Firestore database
    private FirebaseAuth auth = FirebaseAuth.getInstance();          // Authentication service

    /**
     * Called when the activity is first created.
     * Sets up the UI components and initializes data loading.
     * @param b Saved instance state bundle (not used in this case)
     */
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);  // Call parent class onCreate
        setContentView(R.layout.activity_admin_dashboard);  // Set the layout file

        // Initialize TextView references from layout
        tvPending = findViewById(R.id.tvPendingCount);     // Find pending requests count view
        tvAvailable = findViewById(R.id.tvAvailableCount); // Find available cars count view
        tvTotal = findViewById(R.id.tvTotalCars);          // Find total cars count view

        // Set click listeners for dashboard cards/buttons:

        // 1. Manage Cars card - opens car management activity
        findViewById(R.id.cardManageCars).setOnClickListener(v ->
                startActivity(new Intent(this, ManageCarsActivity.class)));

        // 2. View Requests card - opens requests management activity
        findViewById(R.id.cardViewRequests).setOnClickListener(v ->
                startActivity(new Intent(this, ViewRequestsActivity.class)));

        // 3. Logout button - triggers logout process
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        fetchStats(); // Load and display initial statistics
    }

    /**
     * Fetches statistics from Firestore and updates the UI.
     * Gets three key metrics: pending requests, available cars, and total cars.
     */
    private void fetchStats() {
        // 1. Get count of pending rental requests:
        db.collection("rental_requests")          // Access rental_requests collection
                .whereEqualTo("status", "pending")      // Filter for pending status
                .get()                                  // Execute query
                .addOnSuccessListener(snap ->           // On success:
                        tvPending.setText(String.valueOf(snap.size())))  // Update pending count
                .addOnFailureListener(e ->              // On failure:
                        tvPending.setText("?"));            // Show "?" on error

        // 2. Get count of available cars:
        db.collection("cars")                     // Access cars collection
                .whereEqualTo("available", true)        // Filter for available cars
                .get()
                .addOnSuccessListener(snap ->
                        tvAvailable.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e ->
                        tvAvailable.setText("?"));

        // 3. Get total count of all cars:
        db.collection("cars")                     // Access cars collection
                .get()                                  // Get all documents
                .addOnSuccessListener(snap ->
                        tvTotal.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e ->
                        tvTotal.setText("?"));
    }

    /**
     * Handles admin logout process:
     * 1. Signs out from Firebase Auth
     * 2. Redirects to sign-in screen
     * 3. Clears activity stack
     */
    private void logout() {
        auth.signOut();  // Sign out current admin user

        // Create intent for SignInActivity with cleared back stack
        startActivity(new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |  // Clear all previous activities
                        Intent.FLAG_ACTIVITY_NEW_TASK));     // Start new task

        finish();  // Close current activity
    }
}