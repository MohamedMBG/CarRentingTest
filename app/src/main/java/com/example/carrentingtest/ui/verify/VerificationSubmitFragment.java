package com.example.carrentingtest.ui.verify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;
import com.example.carrentingtest.data.MockVerificationService;
import com.example.carrentingtest.data.VerificationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.analytics.FirebaseAnalytics;

public class VerificationSubmitFragment extends Fragment {
    private ProgressBar progressBar;
    private FirebaseAnalytics analytics;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verification_submit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.findViewById(R.id.progress);
        analytics = FirebaseAnalytics.getInstance(requireContext());

        VerificationService service = new MockVerificationService(true);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        analytics.logEvent("verification_submitted", null);
        service.submitVerification(uid, new VerificationService.Callback() {
            @Override
            public void onSuccess() {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("verification_status", "verified",
                                "verified_at", FieldValue.serverTimestamp())
                        .addOnSuccessListener(aVoid -> {
                            analytics.logEvent("verification_success", null);
                            Toast.makeText(requireContext(), R.string.verify_success, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), R.string.verify_fail, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFailure(@NonNull String reasonCode) {
                Bundle bundle = new Bundle();
                bundle.putString("reason", reasonCode);
                analytics.logEvent("verification_fail", bundle);
                Toast.makeText(requireContext(), R.string.verify_fail, Toast.LENGTH_SHORT).show();
            }
        });
    }
}


