package com.example.carrentingtest.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.carrentingtest.domain.UserRole;
import com.example.carrentingtest.utils.NotificationHelper;
import com.example.carrentingtest.verification.VerificationStatus;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class NotificationCheckWorker extends Worker {

    private static final String PREFS = "notif_prefs";
    private static final String KEY_IDS = "notified_ids";
    private static final int MAX_IDS = 300;

    public NotificationCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            DocumentSnapshot userDoc = Tasks.await(db.collection("users").document(uid).get());
            if (!userDoc.exists()) return Result.success();

            String role = userDoc.getString("role");
            String companyId = userDoc.getString("companyId");

            SharedPreferences prefs = getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Set<String> notifiedIds = new HashSet<>(
                    prefs.getStringSet(KEY_IDS, new HashSet<>()));

            if (UserRole.from(role) == UserRole.ADMIN && companyId != null) {
                checkAdminNotifications(db, companyId, notifiedIds);
            } else {
                checkUserNotifications(db, uid, notifiedIds);
            }

            pruneAndSave(prefs, notifiedIds);

        } catch (ExecutionException | InterruptedException e) {
            return Result.retry();
        }

        return Result.success();
    }

    private void checkUserNotifications(FirebaseFirestore db, String uid, Set<String> notifiedIds)
            throws ExecutionException, InterruptedException {

        // Booking approved / rejected
        QuerySnapshot rentalSnap = Tasks.await(
                db.collection("rental_requests")
                        .whereEqualTo("userId", uid)
                        .whereIn("status", Arrays.asList("approved", "rejected"))
                        .get());

        for (DocumentSnapshot doc : rentalSnap.getDocuments()) {
            String id = "rental_" + doc.getId();
            if (!notifiedIds.contains(id)) {
                String status = doc.getString("status");
                String car = doc.getString("carModel");
                String carLabel = car != null ? car : "a vehicle";
                boolean approved = "approved".equals(status);
                NotificationHelper.showBookingNotification(
                        getApplicationContext(),
                        approved ? "Booking Approved" : "Booking Rejected",
                        approved
                                ? "Your request for " + carLabel + " has been approved."
                                : "Your request for " + carLabel + " was not approved.");
                notifiedIds.add(id);
            }
        }

        // Verification approved / rejected
        DocumentSnapshot verifDoc = Tasks.await(
                db.collection("verification_requests").document(uid).get());
        if (verifDoc.exists()) {
            String status = verifDoc.getString("status");
            boolean isApproved = VerificationStatus.APPROVED.getStorageValue().equals(status);
            boolean isRejected = VerificationStatus.REJECTED.getStorageValue().equals(status);
            if (isApproved || isRejected) {
                String verifKey = "verif_" + uid + "_" + status;
                if (!notifiedIds.contains(verifKey)) {
                    NotificationHelper.showVerificationNotification(
                            getApplicationContext(),
                            isApproved ? "Verification Approved" : "Verification Rejected",
                            isApproved
                                    ? "Your identity verification was approved. You can now rent vehicles."
                                    : "Your identity verification was rejected. Please review and resubmit.");
                    notifiedIds.add(verifKey);
                }
            }
        }
    }

    private void checkAdminNotifications(FirebaseFirestore db, String companyId,
                                         Set<String> notifiedIds)
            throws ExecutionException, InterruptedException {

        // New verification requests under review
        QuerySnapshot verifSnap = Tasks.await(
                db.collection("verification_requests")
                        .whereEqualTo("companyId", companyId)
                        .whereEqualTo("status", VerificationStatus.UNDER_REVIEW.getStorageValue())
                        .get());

        List<String> newVerifUsers = new ArrayList<>();
        for (DocumentSnapshot doc : verifSnap.getDocuments()) {
            String id = "verif_" + doc.getId();
            if (!notifiedIds.contains(id)) {
                String name = doc.getString("userName");
                newVerifUsers.add(name != null ? name : "A user");
                notifiedIds.add(id);
            }
        }
        if (!newVerifUsers.isEmpty()) {
            boolean single = newVerifUsers.size() == 1;
            NotificationHelper.showVerificationNotification(
                    getApplicationContext(),
                    single ? "New Verification Request" : newVerifUsers.size() + " New Verification Requests",
                    single
                            ? newVerifUsers.get(0) + " submitted documents for review."
                            : newVerifUsers.size() + " users submitted documents for review.");
        }

        // New pending rental requests
        QuerySnapshot rentalSnap = Tasks.await(
                db.collection("rental_requests")
                        .whereEqualTo("companyId", companyId)
                        .whereEqualTo("status", "pending")
                        .get());

        List<String> newRentalCars = new ArrayList<>();
        for (DocumentSnapshot doc : rentalSnap.getDocuments()) {
            String id = "rental_" + doc.getId();
            if (!notifiedIds.contains(id)) {
                String car = doc.getString("carModel");
                newRentalCars.add(car != null ? car : "a vehicle");
                notifiedIds.add(id);
            }
        }
        if (!newRentalCars.isEmpty()) {
            boolean single = newRentalCars.size() == 1;
            NotificationHelper.showBookingNotification(
                    getApplicationContext(),
                    single ? "New Rental Request" : newRentalCars.size() + " New Rental Requests",
                    single
                            ? "Request for " + newRentalCars.get(0) + " awaiting your approval."
                            : newRentalCars.size() + " rental requests awaiting your approval.");
        }
    }

    private void pruneAndSave(SharedPreferences prefs, Set<String> ids) {
        Set<String> pruned = ids;
        if (ids.size() > MAX_IDS) {
            List<String> list = new ArrayList<>(ids);
            pruned = new HashSet<>(list.subList(list.size() - MAX_IDS, list.size()));
        }
        prefs.edit().putStringSet(KEY_IDS, pruned).apply();
    }
}
