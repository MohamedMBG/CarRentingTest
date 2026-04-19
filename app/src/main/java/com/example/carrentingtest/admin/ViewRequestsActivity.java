package com.example.carrentingtest.admin;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.EmailSender;
import com.example.carrentingtest.R;
import com.example.carrentingtest.domain.RentalRequestStatus;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.pricing.PricingService;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {
    private RecyclerView requestsRecyclerView;
    private View emptyStateView;
    private RentalRequestAdapter adapter;
    private List<RentalRequest> requestList = new ArrayList<>();
    private String companyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);
        FullscreenUiHelper.apply(this, R.id.view_requests_root);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        emptyStateView = findViewById(R.id.emptyStateView);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RentalRequestAdapter(requestList, this::handleRequestDecision);
        requestsRecyclerView.setAdapter(adapter);

        AdminAccessManager.guardOperationalAccess(this, FirebaseFirestore.getInstance(), access -> {
            companyId = access.getCompanyId();
            loadRequests();
        });
    }

    private void loadRequests() {
        if (companyId == null)
            return;
        FirebaseFirestore.getInstance().collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        updateEmptyState();
                        return;
                    }

                    requestList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        RentalRequest request = doc.toObject(RentalRequest.class);
                        request.setRequestId(doc.getId());
                        requestList.add(request);
                    }
                    Collections.sort(requestList, Comparator.comparing(
                            RentalRequest::getStartDate,
                            Comparator.nullsLast(Date::compareTo)
                    ));
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        boolean empty = requestList.isEmpty();
        if (emptyStateView != null) {
            emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private void handleRequestDecision(RentalRequest request, boolean approved) {
        if (approved) {
            approveRequest(request);
            return;
        }

        String newStatus = RentalRequestStatus.REJECTED.getStorageValue();
        FirebaseFirestore.getInstance()
                .collection("rental_requests")
                .document(request.getRequestId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    sendEmailNotification(request, newStatus);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void approveRequest(RentalRequest request) {
        FirebaseFirestore.getInstance()
                .collection("cars")
                .document(request.getCarId())
                .get()
                .addOnSuccessListener(carDoc -> {
                    Car car = carDoc.toObject(Car.class);
                    if (car == null) {
                        Toast.makeText(this, "Failed to load car pricing.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    car.setDocumentId(carDoc.getId());

                    if (car.isMaintenance()) {
                        Toast.makeText(this, R.string.request_car_maintenance, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!car.isAvailable()) {
                        Toast.makeText(this, R.string.request_car_unavailable, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    verifyNoConflictingApproval(request, car);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to load car pricing: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void verifyNoConflictingApproval(RentalRequest request, Car car) {
        FirebaseFirestore.getInstance()
                .collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("carId", request.getCarId())
                .whereEqualTo("status", RentalRequestStatus.APPROVED.getStorageValue())
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (doc.getId().equals(request.getRequestId())) {
                            continue;
                        }
                        RentalRequest approvedRequest = doc.toObject(RentalRequest.class);
                        if (approvedRequest != null && datesOverlap(
                                request.getStartDate(),
                                request.getEndDate(),
                                approvedRequest.getStartDate(),
                                approvedRequest.getEndDate())) {
                            Toast.makeText(this, R.string.request_no_overlap, Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    finalizeApproval(request, car);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to review overlapping bookings: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void finalizeApproval(RentalRequest request, Car car) {
        PricingService.applyPricing(
                request,
                PricingService.quote(car, request.getStartDate(), request.getEndDate()));

        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        batch.update(
                FirebaseFirestore.getInstance().collection("rental_requests").document(request.getRequestId()),
                "status", RentalRequestStatus.APPROVED.getStorageValue(),
                "totalPrice", request.getTotalPrice(),
                "pricingBreakdown", request.getPricingBreakdown());
        batch.update(
                FirebaseFirestore.getInstance().collection("cars").document(request.getCarId()),
                "available", false);
        batch.commit()
                .addOnSuccessListener(unused -> sendEmailNotification(
                        request,
                        RentalRequestStatus.APPROVED.getStorageValue()))
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to update request: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private boolean datesOverlap(Date firstStart, Date firstEnd, Date secondStart, Date secondEnd) {
        if (firstStart == null || firstEnd == null || secondStart == null || secondEnd == null) {
            return false;
        }
        return firstStart.before(secondEnd) && secondStart.before(firstEnd);
    }

    private void sendEmailNotification(RentalRequest request, String status) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(request.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userEmail = documentSnapshot.getString("email");
                        if (userEmail != null && !userEmail.isEmpty()) {
                            String subject = "Your Rental Request Update - " + request.getCarModel();

                            String body = "Dear " + request.getUserName() + ",\n\n" +
                                    "We're writing to inform you about the status of your rental request:\n\n" +
                                    "Car Model: " + request.getCarModel() + "\n" +
                                    "Rental Period: " + request.getStartDate() + " to " + request.getEndDate() + "\n" +
                                    "Status: " + status.toUpperCase() + "\n\n";

                            if (status.equals("approved")) {
                                body += "Congratulations! Your rental request has been approved.\n" +
                                        "Please visit our office to complete the paperwork and pick up your vehicle.\n\n";
                            } else {
                                body += "We regret to inform you that your rental request could not be approved at this time.\n"
                                        +
                                        "Please feel free to contact us if you have any questions.\n\n";
                            }

                            body += "Thank you for choosing our service.\n\n" +
                                    "Best regards,\n" +
                                    "Car Rental Team";

                            EmailSender.sendEmail(this, userEmail, subject, body, new EmailSender.EmailCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(ViewRequestsActivity.this,
                                            "Request " + status + " and notification sent",
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(ViewRequestsActivity.this,
                                            "Request " + status + " but failed to send email: " + error,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            Toast.makeText(this,
                                    "Request " + status + " but user email not found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Request " + status + " but failed to fetch user email: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
