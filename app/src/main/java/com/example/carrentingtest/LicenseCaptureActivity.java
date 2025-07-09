package com.example.carrentingtest;

import static com.example.carrentingtest.SelfieCaptureActivity.EXTRA_SELFIE_URI;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.carrentingtest.utils.FaceVerificationUtil;
import com.example.carrentingtest.utils.MoroccanLicenseValidator;

import java.io.IOException;

public class LicenseCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_STORAGE = 102;

    private ImageView ivFront, ivBack;
    private Uri frontUri, backUri, selfieUri;

    // Camera launcher
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap image = (Bitmap) result.getData().getExtras().get("data");
                    if (image != null) {
                        try {
                            Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(
                                    getContentResolver(),
                                    image,
                                    "license",
                                    null
                            ));

                            if (frontUri == null) {
                                frontUri = uri;
                                ivFront.setImageBitmap(image);
                            } else {
                                backUri = uri;
                                ivBack.setImageBitmap(image);
                            }
                            updateFinishButtonState();
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    // Gallery launcher
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(),
                                    selectedUri
                            );

                            if (frontUri == null) {
                                frontUri = selectedUri;
                                ivFront.setImageBitmap(bitmap);
                            } else {
                                backUri = selectedUri;
                                ivBack.setImageBitmap(bitmap);
                            }
                            updateFinishButtonState();
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_capture);

        selfieUri = Uri.parse(getIntent().getStringExtra(EXTRA_SELFIE_URI));

        ivFront = findViewById(R.id.ivLicenseFront);
        ivBack = findViewById(R.id.ivLicenseBack);

        Button btnCaptureFront = findViewById(R.id.btnCaptureFront);
        Button btnCaptureBack = findViewById(R.id.btnCaptureBack);
        Button btnUploadFront = findViewById(R.id.btnUploadFront);
        Button btnUploadBack = findViewById(R.id.btnUploadBack);
        Button btnFinish = findViewById(R.id.btnFinishRegistration);

        // Camera capture listeners
        btnCaptureFront.setOnClickListener(v -> launchCamera());
        btnCaptureBack.setOnClickListener(v -> launchCamera());

        // Gallery upload listeners
        btnUploadFront.setOnClickListener(v -> openGallery());
        btnUploadBack.setOnClickListener(v -> openGallery());

        btnFinish.setOnClickListener(v -> verifyAndFinish());
        updateFinishButtonState();
    }

    private void updateFinishButtonState() {
        Button btnFinish = findViewById(R.id.btnFinishRegistration);
        boolean isComplete = (selfieUri != null && frontUri != null);
        btnFinish.setEnabled(isComplete);
        btnFinish.setAlpha(isComplete ? 1.0f : 0.5f);
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA
            );
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        }
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE
            );
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }

    private void verifyAndFinish() {
        if (selfieUri == null || frontUri == null) {
            Toast.makeText(this, "Please provide selfie and front license", Toast.LENGTH_LONG).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying documents...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                Bitmap selfieBitmap = getBitmapFromUri(this, selfieUri);
                Bitmap licenseBitmap = getBitmapFromUri(this, frontUri);

                if (selfieBitmap == null || licenseBitmap == null) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to load images", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                boolean facesMatch = FaceVerificationUtil.isFaceMatch(
                        selfieBitmap,
                        licenseBitmap,
                        LicenseCaptureActivity.this
                );

                String licenseNumber = getIntent().getStringExtra("driverLicense");
                boolean licenseValid = MoroccanLicenseValidator.isValid(licenseNumber);

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (facesMatch && licenseValid) {
                        Intent result = new Intent();
                        result.putExtra(EXTRA_SELFIE_URI, selfieUri.toString());
                        result.putExtra("license_front", frontUri.toString());
                        if (backUri != null) {
                            result.putExtra("license_back", backUri.toString());
                        }
                        setResult(RESULT_OK, result);
                        finish();
                    } else {
                        String error = "Verification failed - ";
                        if (!facesMatch) error += "Faces don't match. ";
                        if (!licenseValid) error += "Invalid license number.";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("LicenseCapture", "Verification error", e);
                });
            }
        }).start();
    }

    public static Bitmap getBitmapFromUri(android.content.Context context, Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (Exception e) {
            Log.e("LicenseCapture", "Failed to load bitmap from URI", e);
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == REQUEST_CAMERA) {
            launchCamera();
        } else if (requestCode == REQUEST_STORAGE) {
            openGallery();
        }
    }
}