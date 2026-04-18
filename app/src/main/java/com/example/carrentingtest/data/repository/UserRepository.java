package com.example.carrentingtest.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class UserRepository {

    private final FirebaseFirestore firestore;

    public UserRepository() {
        this(FirebaseFirestore.getInstance());
    }

    UserRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<DocumentSnapshot> getById(@Nullable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("User id is required."));
        }
        return firestore.collection("users").document(userId).get();
    }

    public Task<DocumentSnapshot> getCurrentUserDocument() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return Tasks.forException(new IllegalStateException("Authentication required."));
        }
        return getById(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    public Task<DocumentSnapshot> getPrimaryAdminForCompany(@Nullable String companyId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Company id is required."));
        }
        return firestore.collection("users")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("role", "admin")
                .limit(1)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(
                                task.getException() != null
                                        ? task.getException()
                                        : new IllegalStateException("Failed to load admin user."));
                    }
                    return Tasks.forResult(firstDocument(task.getResult()));
                });
    }

    @Nullable
    private DocumentSnapshot firstDocument(@Nullable QuerySnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        return snapshot.getDocuments().get(0);
    }
}
