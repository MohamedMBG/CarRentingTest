package com.example.carrentingtest.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CompanyRepository {

    private final FirebaseFirestore firestore;

    public CompanyRepository() {
        this(FirebaseFirestore.getInstance());
    }

    CompanyRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<DocumentSnapshot> getById(@Nullable String companyId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Company id is required."));
        }
        return firestore.collection("companies").document(companyId).get();
    }
}
