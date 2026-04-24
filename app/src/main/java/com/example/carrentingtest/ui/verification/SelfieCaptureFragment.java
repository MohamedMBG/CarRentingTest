package com.example.carrentingtest.ui.verification;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;

public class SelfieCaptureFragment extends Fragment {
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri pendingSelfieUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_selfie_capture, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.txtHeader)).setText(getString(R.string.selfie_step_header));

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                launchCamera();
            } else {
                Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (Boolean.TRUE.equals(success) && pendingSelfieUri != null
                    && getActivity() instanceof VerificationFlowActivity) {
                com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext())
                        .logEvent("selfie_captured", new android.os.Bundle());
                ((VerificationFlowActivity) getActivity()).onSelfieCaptured(pendingSelfieUri);
            }
        });

        view.findViewById(R.id.btnCapture).setOnClickListener(v -> {
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext()).logEvent("verification_flow_started", new android.os.Bundle());
            ensurePermissionAndLaunch();
        });
    }

    private void ensurePermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        java.io.File file = new java.io.File(requireContext().getCacheDir(), "selfie.jpg");
        pendingSelfieUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file);
        cameraLauncher.launch(pendingSelfieUri);
    }
}


