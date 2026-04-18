package com.example.carrentingtest.data.repository;

import androidx.annotation.NonNull;

import com.example.carrentingtest.models.RentalRequest;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RentalReportRepository {

    private final FirebaseFirestore firestore;

    public RentalReportRepository() {
        this(FirebaseFirestore.getInstance());
    }

    RentalReportRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<?> submitIssueReport(@NonNull RentalRequest request, @NonNull String description) {
        Map<String, Object> report = new HashMap<>();
        report.put("requestId", request.getRequestId());
        report.put("carId", request.getCarId());
        report.put("carModel", request.getCarModel());
        report.put("userId", request.getUserId());
        report.put("userName", request.getUserName());
        report.put("userPhone", request.getUserPhone());
        report.put("companyId", request.getCompanyId());
        report.put("description", description);
        report.put("status", "open");
        report.put("startDate", request.getStartDate());
        report.put("endDate", request.getEndDate());
        report.put("createdAt", FieldValue.serverTimestamp());
        return firestore.collection("rental_reports").add(report);
    }
}
