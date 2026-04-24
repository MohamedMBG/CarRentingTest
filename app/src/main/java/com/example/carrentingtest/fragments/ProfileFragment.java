package com.example.carrentingtest.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.carrentingtest.MainActivity;
import com.example.carrentingtest.R;
import com.example.carrentingtest.SignInActivity;
import com.example.carrentingtest.verification.VerificationStatus;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileLicense, txtVerifiedBadge;
    private TextView tvVerificationPill, tvVerificationHeadline, tvVerificationDescription;
    private Button btnLogout, btnVerifyLicense;
    private MaterialButton btnEditProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);
        tvProfileLicense = view.findViewById(R.id.tvProfileLicense);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnVerifyLicense = view.findViewById(R.id.btnVerifyLicense);
        txtVerifiedBadge = view.findViewById(R.id.txtVerifiedBadge);
        tvVerificationPill = view.findViewById(R.id.tvVerificationPill);
        tvVerificationHeadline = view.findViewById(R.id.tvVerificationHeadline);
        tvVerificationDescription = view.findViewById(R.id.tvVerificationDescription);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // Load user data
        loadUserProfile();

        // Analytics: verify_button_shown when applicable will be fired in loadUserProfile based on status

        if (btnVerifyLicense != null) {
            btnVerifyLicense.setOnClickListener(v -> {
                com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext()).logEvent("verify_button_clicked", new Bundle());
                // Launch verification flow activity
                Intent intent = new Intent(requireContext(), com.example.carrentingtest.ui.verification.VerificationFlowActivity.class);
                startActivity(intent);
            });
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v ->
                    Toast.makeText(requireContext(), R.string.profile_edit_unavailable, Toast.LENGTH_SHORT).show());
        }

        // Set logout button click listener
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh to apply potential verification changes
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Log.d(TAG, "User data found: " + document.getData());
                        // Populate TextViews
                        tvProfileName.setText(document.getString("name"));
                        tvProfileEmail.setText(document.getString("email"));
                        tvProfilePhone.setText(document.getString("phone"));
                        tvProfileLicense.setText(document.getString("driverLicense"));

                        VerificationStatus status = VerificationStatus.from(document.getString("verification_status"));
                        boolean isVerified = status.allowsBooking();
                        if (btnVerifyLicense != null) {
                            btnVerifyLicense.setVisibility(isVerified ? View.GONE : View.VISIBLE);
                            if (!isVerified) {
                                FirebaseAnalytics.getInstance(requireContext()).logEvent("verify_button_shown", new Bundle());
                            }
                        }
                        if (txtVerifiedBadge != null) {
                            txtVerifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
                        }
                        bindVerificationState(status);
                    } else {
                        Log.d(TAG, "No such user document");
                        Toast.makeText(getContext(), "User profile data not found.", Toast.LENGTH_SHORT).show();
                        // Set default text or handle error
                        tvProfileName.setText("N/A");
                        tvProfileEmail.setText(currentUser.getEmail()); // Use email from Auth if available
                        tvProfilePhone.setText("N/A");
                        tvProfileLicense.setText("N/A");
                        bindVerificationState(VerificationStatus.NOT_STARTED);
                    }
                } else {
                    Log.e(TAG, "Error getting user document: ", task.getException());
                    Toast.makeText(getContext(), "Error loading profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    // Set default text or handle error
                    tvProfileName.setText("Error");
                    tvProfileEmail.setText("Error");
                    tvProfilePhone.setText("Error");
                    tvProfileLicense.setText("Error");
                    bindVerificationState(VerificationStatus.NOT_STARTED);
                }
            });
        } else {
            // Should not happen if MainActivity checks login status, but handle anyway
            Log.w(TAG, "Current user is null in ProfileFragment");
            Toast.makeText(getContext(), "Not logged in.", Toast.LENGTH_SHORT).show();
            // Optionally redirect to login
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logoutUser(); // Use existing logout logic
            }
        }
    }

    private void bindVerificationState(VerificationStatus status) {
        if (!isAdded()) {
            return;
        }

        int pillBackground;
        int pillTextColor;
        int headline;
        int description;
        int pillText;

        switch (status) {
            case APPROVED:
                pillBackground = R.drawable.bg_status_approved;
                pillTextColor = R.color.colorSuccess;
                headline = R.string.profile_verification_approved;
                description = R.string.profile_verification_approved_body;
                pillText = R.string.profile_verification_status_ready;
                break;
            case SUBMITTED:
                pillBackground = R.drawable.bg_status_pending;
                pillTextColor = R.color.colorWarning;
                headline = R.string.profile_verification_submitted;
                description = R.string.profile_verification_submitted_body;
                pillText = R.string.profile_verification_status_pending;
                break;
            case UNDER_REVIEW:
                pillBackground = R.drawable.bg_status_pending;
                pillTextColor = R.color.colorWarning;
                headline = R.string.profile_verification_under_review;
                description = R.string.profile_verification_under_review_body;
                pillText = R.string.profile_verification_status_pending;
                break;
            case REJECTED:
                pillBackground = R.drawable.bg_status_rejected;
                pillTextColor = R.color.colorError;
                headline = R.string.profile_verification_rejected;
                description = R.string.profile_verification_rejected_body;
                pillText = R.string.profile_verification_status_pending;
                break;
            case NOT_STARTED:
            default:
                pillBackground = R.drawable.bg_badge_soft;
                pillTextColor = R.color.colorPrimary;
                headline = R.string.profile_verification_not_started;
                description = R.string.profile_verification_not_started_body;
                pillText = R.string.profile_verification_status_pending;
                break;
        }

        tvVerificationPill.setBackgroundResource(pillBackground);
        tvVerificationPill.setText(pillText);
        tvVerificationPill.setTextColor(requireContext().getColor(pillTextColor));
        tvVerificationHeadline.setText(headline);
        tvVerificationDescription.setText(description);
    }

    private void logout() {
        // Call the logout method in MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).logoutUser();
        } else {
            // Fallback if not attached to MainActivity (should not happen)
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
