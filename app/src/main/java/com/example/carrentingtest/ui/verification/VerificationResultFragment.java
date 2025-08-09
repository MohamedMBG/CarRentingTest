package com.example.carrentingtest.ui.verification;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.carrentingtest.R;
import com.example.carrentingtest.verification.data.VerificationResult;
import com.example.carrentingtest.verification.data.VerificationService;
import com.example.carrentingtest.verification.data.MockVerificationService;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class VerificationResultFragment extends Fragment {

    private VerificationViewModel viewModel;
    private VerificationService verificationService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verification_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VerificationViewModel.class);

        TextView txt = view.findViewById(R.id.txtResult);

        verificationService = new MockVerificationService(requireContext());

        Uri selfie = viewModel.getSelfieUri();
        Uri license = viewModel.getLicenseUri();

        if (selfie == null || license == null) {
            txt.setText(getString(R.string.verification_failed));
            return;
        }

        viewModel.setSubmitting(true);
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext()).logEvent("verification_submitted", new android.os.Bundle());
        verificationService.submit(selfie, license).observe(getViewLifecycleOwner(), result -> {
            viewModel.setSubmitting(false);
            if (result == null) return;

            txt.setText(mapStatusMessage(result.getStatus()));
            android.os.Bundle params = new android.os.Bundle();
            params.putString("status", result.getStatus().name());
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext()).logEvent("verification_result", params);
            if (result.getStatus() == VerificationResult.Status.VERIFIED) {
                updateUserAsVerified();
                requireActivity().setResult(android.app.Activity.RESULT_OK);
                requireActivity().finish();
            }
        });
    }

    private CharSequence mapStatusMessage(VerificationResult.Status status) {
        switch (status) {
            case VERIFIED: return getString(R.string.verification_success);
            case PENDING: return getString(R.string.verification_pending);
            case FAILED:
            default: return getString(R.string.verification_failed);
        }
    }

    private void updateUserAsVerified() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(
                        "verification_status", "VERIFIED",
                        "verification_updated_at", FieldValue.serverTimestamp()
                );
    }
}


