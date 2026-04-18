package com.example.carrentingtest.data.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.carrentingtest.data.repository.CompanyRepository;
import com.example.carrentingtest.data.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class TenantSessionProvider {

    private final FirebaseAuth auth;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public TenantSessionProvider() {
        this(FirebaseAuth.getInstance(), new UserRepository(), new CompanyRepository());
    }

    TenantSessionProvider(@NonNull FirebaseAuth auth,
                          @NonNull UserRepository userRepository,
                          @NonNull CompanyRepository companyRepository) {
        this.auth = auth;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    public Task<TenantContext> requireTenantContext() {
        return requireTenantContext(auth.getCurrentUser());
    }

    public Task<TenantContext> requireTenantContext(@Nullable FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            return Tasks.forException(new IllegalStateException("Authentication required."));
        }

        return userRepository.getById(firebaseUser.getUid())
                .continueWithTask(userTask -> {
                    DocumentSnapshot userDocument = userTask.getResult();
                    if (userDocument == null || !userDocument.exists()) {
                        return Tasks.forException(new IllegalStateException("User session could not be loaded."));
                    }

                    String companyId = userDocument.getString("companyId");
                    if (companyId == null || companyId.trim().isEmpty()) {
                        return Tasks.forException(new IllegalStateException("No tenant is linked to this account."));
                    }

                    return companyRepository.getById(companyId)
                            .continueWith(companyTask ->
                                    TenantContext.from(firebaseUser, userDocument, companyTask.getResult()));
                });
    }
}
