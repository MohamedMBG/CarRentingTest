package com.example.carrentingtest.ui.verify;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;
import com.example.carrentingtest.storage.StoragePaths;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class CaptureLicenseFragment extends Fragment {
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private boolean capturingBack = false;
    private FirebaseAnalytics analytics;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_capture_license, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        analytics = FirebaseAnalytics.getInstance(requireContext());
        previewView = view.findViewById(R.id.previewView);
        view.findViewById(R.id.btnCaptureFront).setOnClickListener(v -> captureAndUpload(false));
        view.findViewById(R.id.btnCaptureBack).setOnClickListener(v -> captureAndUpload(true));

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void captureAndUpload(boolean isBack) {
        if (imageCapture == null) return;
        try {
            File cacheDir = requireContext().getCacheDir();
            File photoFile = File.createTempFile(isBack ? "license_back" : "license_front", ".jpg", cacheDir);
            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
            imageCapture.takePicture(options, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    upload(photoFile, isBack);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(requireContext(), R.string.capture_failed, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.capture_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void upload(File file, boolean isBack) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        String path = isBack ? StoragePaths.licenseBackPath(uid) : StoragePaths.licenseFrontPath(uid);
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);
        ref.putFile(android.net.Uri.fromFile(file))
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(requireContext(), R.string.license_uploaded, Toast.LENGTH_SHORT).show();
                    analytics.logEvent("verification_license_captured", null);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.verification_container, new VerificationSubmitFragment())
                            .addToBackStack(null)
                            .commit();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), R.string.upload_failed, Toast.LENGTH_SHORT).show());
    }
}


