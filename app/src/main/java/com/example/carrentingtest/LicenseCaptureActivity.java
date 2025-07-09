package com.example.carrentingtest;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.carrentingtest.utils.FaceVerificationUtil;
import com.example.carrentingtest.utils.MoroccanLicenseValidator;

/**
 * Activity to capture images of both sides of the driving licence and
 * trigger verification.
 */
public class LicenseCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 101;
    private ImageView ivFront, ivBack;
    private Uri frontUri, backUri, selfieUri;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap image = (Bitmap) result.getData().getExtras().get("data");
                    if (image != null) {
                        try {
                            Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), image, "license", null));
                            if (frontUri == null) {
                                frontUri = uri;
                                ivFront.setImageBitmap(image);
                            } else {
                                backUri = uri;
                                ivBack.setImageBitmap(image);
                            }
                            updateFinishButtonState(findViewById(R.id.btnFinishRegistration));
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_capture);

        selfieUri = Uri.parse(getIntent().getStringExtra(SelfieCaptureActivity.EXTRA_SELFIE_URI));

        ivFront = findViewById(R.id.ivLicenseFront);
        ivBack = findViewById(R.id.ivLicenseBack);
        Button btnCaptureFront = findViewById(R.id.btnCaptureFront);
        Button btnCaptureBack = findViewById(R.id.btnCaptureBack);
        Button btnFinish = findViewById(R.id.btnFinishRegistration);

        // Initially disable the finish button
        btnFinish.setEnabled(false);

        btnCaptureFront.setOnClickListener(v -> launchCamera());
        btnCaptureBack.setOnClickListener(v -> launchCamera());
        btnFinish.setOnClickListener(v -> verifyAndFinish());

        // Update button state whenever images change
        View.OnClickListener captureListener = v -> {
            launchCamera();
            updateFinishButtonState(btnFinish);
        };

        btnCaptureFront.setOnClickListener(captureListener);
        btnCaptureBack.setOnClickListener(captureListener);
    }

    private void updateFinishButtonState(Button btnFinish) {
        // Enable button only when both selfie and front license are captured
        boolean isComplete = (selfieUri != null && frontUri != null);
        btnFinish.setEnabled(isComplete);

        // Visual feedback
        btnFinish.setAlpha(isComplete ? 1.0f : 0.5f);
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        }
    }

    private void verifyAndFinish() {
        // Debug logging
        Log.d("LicenseCapture", "verifyAndFinish called");
        Log.d("LicenseCapture", "Selfie URI: " + (selfieUri != null ? selfieUri.toString() : "null"));
        Log.d("LicenseCapture", "Front URI: " + (frontUri != null ? frontUri.toString() : "null"));
        Log.d("LicenseCapture", "Back URI: " + (backUri != null ? backUri.toString() : "null"));

        if (selfieUri == null || frontUri == null) {
            String missing = "";
            if (selfieUri == null) missing += "selfie, ";
            if (frontUri == null) missing += "front license photo";
            Toast.makeText(this, "Please capture: " + missing, Toast.LENGTH_LONG).show();
            return;
        }

        // Show loading indicator
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying documents...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            boolean facesMatch = FaceVerificationUtil.verifyFaces(this, selfieUri, frontUri);
            String licenseNumber = getIntent().getStringExtra("driverLicense");
            boolean licenseValid = MoroccanLicenseValidator.isValid(licenseNumber);

            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (facesMatch && licenseValid) {
                    Intent result = new Intent();
                    result.putExtra(SelfieCaptureActivity.EXTRA_SELFIE_URI, selfieUri.toString());
                    result.putExtra("license_front", frontUri.toString());
                    result.putExtra("license_back", backUri != null ? backUri.toString() : null);
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    String message = "Verification failed - ";
                    if (!facesMatch) message += "Faces don't match. ";
                    if (!licenseValid) message += "Invalid license number.";
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }
}