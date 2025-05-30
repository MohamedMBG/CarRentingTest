package com.example.carrentingtest.admin;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.EmailSender;
import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {
    private RecyclerView requestsRecyclerView;
    private RentalRequestAdapter adapter;
    private List<RentalRequest> requestList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_requests);


        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RentalRequestAdapter(requestList, this::handleRequestDecision);
        requestsRecyclerView.setAdapter(adapter);

        loadRequests();
    }

    private void loadRequests() {
        FirebaseFirestore.getInstance().collection("rental_requests")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    requestList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        RentalRequest request = doc.toObject(RentalRequest.class);
                        request.setRequestId(doc.getId());
                        requestList.add(request);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void handleRequestDecision(RentalRequest request, boolean approved) {
        String newStatus = approved ? "approved" : "rejected";

        FirebaseFirestore.getInstance()
                .collection("rental_requests")
                .document(request.getRequestId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (approved) {
                        // Mark car as unavailable if approved
                        FirebaseFirestore.getInstance()
                                .collection("cars")
                                .document(request.getCarId())
                                .update("available", false);
                    }

                    // Send email notification with proper callback
                    sendEmailNotification(request, newStatus);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmailNotification(RentalRequest request, String status) {
        FirebaseFirestore.getInstance()
                .collection("clients")
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
                                body += "We regret to inform you that your rental request could not be approved at this time.\n" +
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