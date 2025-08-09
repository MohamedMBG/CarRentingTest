package com.example.carrentingtest.ui.verification;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;

public class LicenseCaptureFragment extends Fragment {
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<android.content.Intent> cameraLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_license_capture, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.txtHeader)).setText(getString(R.string.license_step_header));

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) launchCamera();
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                android.os.Bundle extras = result.getData().getExtras();
                Uri imageUri = result.getData().getData();
                if (extras != null && extras.get("data") instanceof android.graphics.Bitmap) {
                    android.graphics.Bitmap bmp = (android.graphics.Bitmap) extras.get("data");
                    java.io.File file = new java.io.File(requireContext().getCacheDir(), "license_front.jpg");
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
                    } catch (java.io.IOException ignored) {}
                    imageUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
                }
                if (getActivity() instanceof VerificationFlowActivity && imageUri != null) {
                    com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext()).logEvent("license_captured", new android.os.Bundle());
                    ((VerificationFlowActivity) getActivity()).onLicenseCaptured(imageUri);
                }
            }
        });

        view.findViewById(R.id.btnCapture).setOnClickListener(v -> ensurePermissionAndLaunch());
    }

    private void ensurePermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        android.content.Intent takePictureIntent = new android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cameraLauncher.launch(takePictureIntent);
    }
}


