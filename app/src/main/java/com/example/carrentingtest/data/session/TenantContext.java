package com.example.carrentingtest.data.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.carrentingtest.domain.CompanyLifecycleStatus;
import com.example.carrentingtest.domain.UserLifecycleStatus;
import com.example.carrentingtest.domain.UserRole;
import com.example.carrentingtest.verification.VerificationStatus;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class TenantContext {
    private final String userId;
    private final String companyId;
    private final UserRole role;
    private final UserLifecycleStatus userStatus;
    private final CompanyLifecycleStatus companyStatus;
    private final VerificationStatus verificationStatus;

    public TenantContext(@NonNull String userId,
                         @Nullable String companyId,
                         @NonNull UserRole role,
                         @NonNull UserLifecycleStatus userStatus,
                         @NonNull CompanyLifecycleStatus companyStatus,
                         @NonNull VerificationStatus verificationStatus) {
        this.userId = userId;
        this.companyId = companyId;
        this.role = role;
        this.userStatus = userStatus;
        this.companyStatus = companyStatus;
        this.verificationStatus = verificationStatus;
    }

    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getCompanyId() {
        return companyId;
    }

    public UserRole getRole() {
        return role;
    }

    public UserLifecycleStatus getUserStatus() {
        return userStatus;
    }

    public CompanyLifecycleStatus getCompanyStatus() {
        return companyStatus;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public boolean hasTenantScope() {
        return companyId != null && !companyId.trim().isEmpty();
    }

    @NonNull
    public static TenantContext from(@NonNull FirebaseUser firebaseUser,
                                     @NonNull DocumentSnapshot userDocument,
                                     @Nullable DocumentSnapshot companyDocument) {
        UserRole role = UserRole.from(userDocument.getString("role"));
        UserLifecycleStatus userStatus = UserLifecycleStatus.from(userDocument.getString("status"), role);
        CompanyLifecycleStatus companyStatus = CompanyLifecycleStatus.from(
                companyDocument != null ? companyDocument.getString("status") : null);
        VerificationStatus verificationStatus = VerificationStatus.from(
                userDocument.getString("verification_status"));
        return new TenantContext(
                firebaseUser.getUid(),
                userDocument.getString("companyId"),
                role,
                userStatus,
                companyStatus,
                verificationStatus);
    }
}
