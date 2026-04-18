package com.example.carrentingtest.admin;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.SignInActivity;
import com.example.carrentingtest.data.session.TenantContext;
import com.example.carrentingtest.data.session.TenantSessionProvider;
import com.example.carrentingtest.domain.CompanyLifecycleStatus;
import com.example.carrentingtest.domain.UserLifecycleStatus;
import com.example.carrentingtest.domain.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public final class AdminAccessManager {

    public interface AccessCallback {
        void onGranted(@NonNull AdminAccess access);

        void onDenied(@NonNull String message);
    }

    public static final class AdminAccess {
        private final String companyId;
        private final UserLifecycleStatus userStatus;
        private final CompanyLifecycleStatus companyStatus;

        AdminAccess(@NonNull String companyId,
                    @NonNull UserLifecycleStatus userStatus,
                    @NonNull CompanyLifecycleStatus companyStatus) {
            this.companyId = companyId;
            this.userStatus = userStatus;
            this.companyStatus = companyStatus;
        }

        public String getCompanyId() {
            return companyId;
        }

        public UserLifecycleStatus getUserStatus() {
            return userStatus;
        }

        public CompanyLifecycleStatus getCompanyStatus() {
            return companyStatus;
        }
    }

    private AdminAccessManager() {}

    public static void verifyOperationalAccess(@NonNull FirebaseFirestore db,
                                               @Nullable FirebaseUser firebaseUser,
                                               @NonNull AccessCallback callback) {
        new TenantSessionProvider()
                .requireTenantContext(firebaseUser)
                .addOnSuccessListener(context -> verifyCompany(context, callback))
                .addOnFailureListener(e -> callback.onDenied(
                        e != null && e.getMessage() != null ? e.getMessage() : "Failed to load admin account."));
    }

    public static void guardOperationalAccess(@NonNull AppCompatActivity activity,
                                              @NonNull FirebaseFirestore db,
                                              @NonNull GrantedAction grantedAction) {
        verifyOperationalAccess(db, FirebaseAuth.getInstance().getCurrentUser(), new AccessCallback() {
            @Override
            public void onGranted(@NonNull AdminAccess access) {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    grantedAction.onGranted(access);
                }
            }

            @Override
            public void onDenied(@NonNull String message) {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                    redirectToSignIn(activity);
                }
            }
        });
    }

    public static void redirectToSignIn(@NonNull Context context) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(context, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).finish();
        }
    }

    private static void verifyCompany(@NonNull TenantContext context,
                                      @NonNull AccessCallback callback) {
        UserRole role = context.getRole();
        if (role != UserRole.ADMIN) {
            callback.onDenied("This account does not have admin access.");
            return;
        }

        UserLifecycleStatus userStatus = context.getUserStatus();
        if (userStatus != UserLifecycleStatus.ACTIVE) {
            callback.onDenied("Your company account is still waiting for approval.");
            return;
        }

        String companyId = context.getCompanyId();
        if (companyId == null || companyId.trim().isEmpty()) {
            callback.onDenied("No company is linked to this admin account.");
            return;
        }

        CompanyLifecycleStatus companyStatus = context.getCompanyStatus();
        if (companyStatus != CompanyLifecycleStatus.APPROVED) {
            callback.onDenied("Your company account is not approved for operations yet.");
            return;
        }
        callback.onGranted(new AdminAccess(companyId, userStatus, companyStatus));
    }

    public interface GrantedAction {
        void onGranted(@NonNull AdminAccess access);
    }
}
