package com.example.carrentingtest.ui.verification;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.carrentingtest.R;
import com.example.carrentingtest.verification.VerificationStatus;
import com.example.carrentingtest.verification.data.FirebaseVerificationService;
import com.example.carrentingtest.verification.data.VerificationResult;
import com.example.carrentingtest.verification.data.VerificationService;
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
        TextView detail = view.findViewById(R.id.txtResultDetail);
        ImageView icon = view.findViewById(R.id.imgResultIcon);

        verificationService = new FirebaseVerificationService(requireContext());

        Uri selfie = viewModel.getSelfieUri();
        Uri license = viewModel.getLicenseUri();

        if (selfie == null || license == null) {
            bindResultState(
                    new VerificationResult(
                            VerificationResult.Status.REJECTED,
                            0.0,
                            getString(R.string.verification_failed)),
                    txt,
                    detail,
                    icon);
            return;
        }

        viewModel.setSubmitting(true);
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext()).logEvent("verification_submitted", new android.os.Bundle());
        verificationService.submit(selfie, license).observe(getViewLifecycleOwner(), result -> {
            viewModel.setSubmitting(false);
            if (result == null) return;

            bindResultState(result, txt, detail, icon);
            android.os.Bundle params = new android.os.Bundle();
            params.putString("status", result.getStatus().name());
            params.putDouble("face_match_score", result.getFaceMatchScore());
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext()).logEvent("verification_result", params);
            updateUserVerificationStatus(result.getStatus());
            if (result.getStatus() == VerificationResult.Status.APPROVED) {
                requireActivity().setResult(android.app.Activity.RESULT_OK);
                requireActivity().finish();
            }
        });
    }

    private void bindResultState(@NonNull VerificationResult result,
                                 @NonNull TextView title,
                                 @NonNull TextView detail,
                                 @NonNull ImageView icon) {
        title.setText(mapStatusMessage(result));

        int iconDrawable;
        int iconBackground;
        int iconTint;
        int titleColor;
        int detailText;

        switch (result.getStatus()) {
            case APPROVED:
                iconDrawable = R.drawable.ic_check_circle;
                iconBackground = R.drawable.bg_result_success;
                iconTint = R.color.colorSuccess;
                titleColor = R.color.colorSuccess;
                detailText = R.string.profile_verification_approved_body;
                break;
            case SUBMITTED:
            case UNDER_REVIEW:
                iconDrawable = R.drawable.ic_clock_status;
                iconBackground = R.drawable.bg_result_pending;
                iconTint = R.color.colorWarning;
                titleColor = R.color.textColorPrimary;
                detailText = R.string.profile_verification_under_review_body;
                break;
            case REJECTED:
            default:
                iconDrawable = R.drawable.ic_alert_circle;
                iconBackground = R.drawable.bg_result_error;
                iconTint = R.color.colorError;
                titleColor = R.color.colorError;
                detailText = R.string.profile_verification_rejected_body;
                break;
        }

        icon.setImageResource(iconDrawable);
        icon.setBackgroundResource(iconBackground);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), iconTint));
        title.setTextColor(ContextCompat.getColor(requireContext(), titleColor));
        detail.setText(detailText);
    }

    private CharSequence mapStatusMessage(VerificationResult result) {
        switch (result.getStatus()) {
            case APPROVED:
                return getString(R.string.verification_success_with_score, result.getFaceMatchScore() * 100.0);
            case SUBMITTED:
            case UNDER_REVIEW:
                return getString(R.string.verification_pending);
            case REJECTED:
            default:
                String message = result.getMessage();
                return message != null && !message.trim().isEmpty()
                        ? getString(R.string.verification_failed_with_reason, message)
                        : getString(R.string.verification_failed);
        }
    }

    private void updateUserVerificationStatus(VerificationResult.Status status) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        VerificationStatus verificationStatus;
        switch (status) {
            case APPROVED:
                verificationStatus = VerificationStatus.APPROVED;
                break;
            case REJECTED:
                verificationStatus = VerificationStatus.REJECTED;
                break;
            case UNDER_REVIEW:
                verificationStatus = VerificationStatus.UNDER_REVIEW;
                break;
            case SUBMITTED:
            default:
                verificationStatus = VerificationStatus.SUBMITTED;
                break;
        }
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(
                        "verification_status", verificationStatus.getStorageValue(),
                        "verification_updated_at", FieldValue.serverTimestamp()
                );
    }
}


