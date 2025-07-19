package com.example.carrentingtest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.utils.FaceNetUtil;
import com.example.carrentingtest.utils.MoroccanLicenseValidator;

import java.io.IOException;
import java.util.concurrent.Executors;

public class LicenseCaptureActivity extends AppCompatActivity {
    private static final float MATCH_THRESHOLD = 0.7f;

    // Constants to track which side we're uploading
    private static final int UPLOAD_FRONT = 1;
    private static final int UPLOAD_BACK = 2;
    private int currentUploadSide = UPLOAD_FRONT;

    private ImageView ivFrontLicense, ivBackLicense;
    private TextView tvResult;
    private Button btnUploadFront, btnUploadBack, btnVerify;
    private Bitmap selfieBitmap, frontLicenseBitmap, backLicenseBitmap;
    private FaceNetUtil faceNetUtil;
    private String driverLicenseNumber;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    try {
                        Uri uri = result.getData().getData();
                        Bitmap licenseBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                        if (currentUploadSide == UPLOAD_FRONT) {
                            frontLicenseBitmap = licenseBitmap;
                            ivFrontLicense.setImageBitmap(frontLicenseBitmap);
                            Toast.makeText(this, "Front side uploaded", Toast.LENGTH_SHORT).show();
                        } else {
                            backLicenseBitmap = licenseBitmap;
                            ivBackLicense.setImageBitmap(backLicenseBitmap);
                            Toast.makeText(this, "Back side uploaded", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_capture);

        // Initialize FaceNet utility
        faceNetUtil = FaceNetUtil.create(this);

        // Get data from previous activity
        selfieBitmap = getIntent().getParcelableExtra("selfie_bitmap");
        driverLicenseNumber = getIntent().getStringExtra("driverLicense");

        // Initialize views
        ivFrontLicense = findViewById(R.id.ivFrontLicense);
        ivBackLicense = findViewById(R.id.ivBackLicense);
        tvResult = findViewById(R.id.tvVerificationResult);
        btnUploadFront = findViewById(R.id.btnUploadFront);
        btnUploadBack = findViewById(R.id.btnUploadBack);
        btnVerify = findViewById(R.id.btnVerify);

        // Set click listeners
        btnUploadFront.setOnClickListener(v -> {
            currentUploadSide = UPLOAD_FRONT;
            launchImagePicker();
        });

        btnUploadBack.setOnClickListener(v -> {
            currentUploadSide = UPLOAD_BACK;
            launchImagePicker();
        });

        btnVerify.setOnClickListener(v -> verifyLicenseAndFaces());
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void verifyLicenseAndFaces() {
        // First validate license format
        if (!MoroccanLicenseValidator.isValid(driverLicenseNumber)) {
            tvResult.setText("Invalid Moroccan license format");
            return;
        }

        // Check if both sides are uploaded
        if (frontLicenseBitmap == null) {
            tvResult.setText("Please upload front side of license");
            return;
        }

        if (backLicenseBitmap == null) {
            tvResult.setText("Please upload back side of license");
            return;
        }

        tvResult.setText("Verifying license and face...");
        btnVerify.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Verify face against front license (which typically has photo)
                float[] embSelfie = faceNetUtil.getEmbedding(selfieBitmap);
                float[] embLicense = faceNetUtil.getEmbedding(frontLicenseBitmap);
                float similarity = FaceNetUtil.cosineSimilarity(embSelfie, embLicense);
                boolean match = similarity > MATCH_THRESHOLD;

                runOnUiThread(() -> {
                    btnVerify.setEnabled(true);
                    if (match) {
                        tvResult.setText(String.format("Verification successful! Similarity: %.2f", similarity));
                        // Return success result
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("verification_result", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        tvResult.setText(String.format("Verification failed. Similarity: %.2f", similarity));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnVerify.setEnabled(true);
                    tvResult.setText("Verification error");
                    Toast.makeText(this, "Face verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up bitmaps to avoid memory leaks
        if (frontLicenseBitmap != null && !frontLicenseBitmap.isRecycled()) {
            frontLicenseBitmap.recycle();
        }
        if (backLicenseBitmap != null && !backLicenseBitmap.isRecycled()) {
            backLicenseBitmap.recycle();
        }
        if (selfieBitmap != null && !selfieBitmap.isRecycled()) {
            selfieBitmap.recycle();
        }
    }
}