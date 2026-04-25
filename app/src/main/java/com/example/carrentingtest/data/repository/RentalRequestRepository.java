package com.example.carrentingtest.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.carrentingtest.models.RentalRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

public class RentalRequestRepository {

    private final FirebaseFirestore firestore;

    public RentalRequestRepository() {
        this(FirebaseFirestore.getInstance());
    }

    RentalRequestRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<Void> create(@NonNull RentalRequest request) {
        DocumentReference documentReference = firestore.collection("rental_requests").document();
        request.setRequestId(documentReference.getId());
        return documentReference.set(request);
    }

    public ListenerRegistration listenForUserHistory(@Nullable String userId,
                                                     @NonNull EventListener<QuerySnapshot> listener) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User id is required.");
        }
        return firestore.collection("rental_requests")
                .whereEqualTo("userId", userId)
                .addSnapshotListener(listener);
    }

    public Task<QuerySnapshot> getApprovedForCar(@Nullable String companyId, @Nullable String carId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Company id is required."));
        }
        if (carId == null || carId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Car id is required."));
        }
        return firestore.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("carId", carId)
                .whereEqualTo("status", com.example.carrentingtest.domain.RentalRequestStatus.APPROVED.getStorageValue())
                .get();
    }
}
