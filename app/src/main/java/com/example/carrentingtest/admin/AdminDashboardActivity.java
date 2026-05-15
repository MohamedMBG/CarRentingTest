package com.example.carrentingtest.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.example.carrentingtest.SignInActivity;
import com.example.carrentingtest.domain.RentalRequestStatus;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.pricing.PricingService;
import com.example.carrentingtest.services.NotificationScheduler;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.example.carrentingtest.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity that displays the admin dashboard with key statistics
 * and provides navigation to admin management features.
 */
public class AdminDashboardActivity extends AppCompatActivity {

        // UI Components for Analytics
        private TextView tvTotalRevenue;
        private TextView tvPendingCount;
        private TextView tvActiveRentals;
        private TextView tvMaintenanceCount;

        // Firebase services instances
        private FirebaseFirestore db = FirebaseFirestore.getInstance(); // Cloud Firestore database
        private FirebaseAuth auth = FirebaseAuth.getInstance(); // Authentication service
        private String companyId;

        /**
         * Called when the activity is first created.
         * Sets up the UI components and initializes data loading.
         * 
         * @param b Saved instance state bundle (not used in this case)
         */
        @Override
        protected void onCreate(Bundle b) {
                super.onCreate(b); // Call parent class onCreate
                setContentView(R.layout.activity_admin_dashboard); // Set the layout file
                FullscreenUiHelper.apply(this, R.id.admin_dashboard_root);

                // Initialize TextView references from layout
                tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
                tvPendingCount = findViewById(R.id.tvPendingCount);
                tvActiveRentals = findViewById(R.id.tvActiveRentals);
                tvMaintenanceCount = findViewById(R.id.tvMaintenanceCount);

                // Set click listeners for dashboard cards/buttons:

                // 1. Manage Cars card - opens car management activity
                findViewById(R.id.cardManageCars)
                                .setOnClickListener(v -> startActivity(new Intent(this, ManageCarsActivity.class)));

                // 2. View Requests card - opens requests management activity
                findViewById(R.id.cardViewRequests)
                                .setOnClickListener(v -> startActivity(new Intent(this, ViewRequestsActivity.class)));

                findViewById(R.id.cardActiveRentals)
                                .setOnClickListener(v -> startActivity(new Intent(this, ActiveRentalsActivity.class)));

                findViewById(R.id.cardPastRentals)
                                .setOnClickListener(v -> startActivity(new Intent(this, PastRentalsActivity.class)));

                findViewById(R.id.cardClientReports)
                                .setOnClickListener(v -> startActivity(new Intent(this, ClientReportsActivity.class)));

                findViewById(R.id.cardAgencyLocation)
                                .setOnClickListener(v -> startActivity(new Intent(this, AdminAgencyLocationActivity.class)));

                findViewById(R.id.cardVerificationReviews)
                                .setOnClickListener(v -> startActivity(new Intent(this, AdminVerificationReviewsActivity.class)));

                // 3. Reports card - opens business reports
                findViewById(R.id.cardViewReports)
                                .setOnClickListener(v -> startActivity(new Intent(this, AdminReportsActivity.class)));

                findViewById(R.id.cardPos)
                                .setOnClickListener(v -> startActivity(new Intent(this, AdminPosActivity.class)));

                // 4. Logout button - triggers logout process
                findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

                NotificationHelper.createChannels(this);
                NotificationScheduler.schedule(this);

                AdminAccessManager.guardOperationalAccess(this, db, access -> {
                        companyId = access.getCompanyId();
                        fetchStats();
                });

        }

        /**
         * Fetches statistics from Firestore and updates the UI real-time.
         */
        private void fetchStats() {
                if (companyId == null)
                        return;

                // 1. Pending Requests Count
                db.collection("rental_requests")
                                .whereEqualTo("companyId", companyId)
                                .whereEqualTo("status", RentalRequestStatus.PENDING.getStorageValue())
                                .addSnapshotListener((snap, e) -> {
                                        if (snap != null) {
                                                tvPendingCount.setText(String.valueOf(snap.size()));
                                        } else {
                                                tvPendingCount.setText("-");
                                        }
                                });

                // 2. Active Rentals & Projected Revenue
                db.collection("rental_requests")
                                .whereEqualTo("companyId", companyId)
                                .addSnapshotListener((snap, e) -> {
                                        if (snap != null) {
                                                int activeCount = 0;
                                                double totalRevenue = 0.0;
                                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                                                        RentalRequest request = doc.toObject(RentalRequest.class);
                                                        RentalRequestStatus status = RentalRequestStatus.from(doc.getString("status"));
                                                        if (status == RentalRequestStatus.APPROVED) {
                                                                activeCount++;
                                                        }
                                                        if (status.isRevenueRecognized()) {
                                                                totalRevenue += PricingService.getStoredTotal(request);
                                                        }
                                                }
                                                tvActiveRentals.setText(String.valueOf(activeCount));
                                                tvTotalRevenue.setText(String.format("$%.2f", totalRevenue));
                                        }
                                });

                // 3. Maintenance Fleet Count
                db.collection("cars")
                                .whereEqualTo("companyId", companyId)
                                .whereEqualTo("maintenance", true)
                                .addSnapshotListener((snap, e) -> {
                                        if (snap != null) {
                                                tvMaintenanceCount.setText(String.valueOf(snap.size()));
                                        } else {
                                                tvMaintenanceCount.setText("0");
                                        }
                                });
        }

        /**
         * Handles admin logout process:
         * 1. Signs out from Firebase Auth
         * 2. Redirects to sign-in screen
         * 3. Clears activity stack
         */
        private void logout() {
                auth.signOut(); // Sign out current admin user

                // Create intent for SignInActivity with cleared back stack
                startActivity(new Intent(this, SignInActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | // Clear all previous activities
                                                Intent.FLAG_ACTIVITY_NEW_TASK)); // Start new task

                finish(); // Close current activity
        }
}
