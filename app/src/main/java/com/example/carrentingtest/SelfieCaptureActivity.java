package com.example.carrentingtest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;

/**
 * Activity to capture a live selfie as part of the registration flow.
 */
public class SelfieCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_SELFIE_URI = "selfie_uri";
    private ImageView ivPreview;
    private Uri selfieUri;

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        try {
                            selfieUri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "selfie", null));
                            ivPreview.setImageBitmap(imageBitmap);
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> licenseLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // pass result back to SignUpActivity
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_capture);

        ivPreview = findViewById(R.id.ivSelfiePreview);
        Button btnCapture = findViewById(R.id.btnCaptureSelfie);
        Button btnNext = findViewById(R.id.btnSelfieNext);

        btnCapture.setOnClickListener(v -> launchCamera());
        btnNext.setOnClickListener(v -> {
            if (selfieUri != null) {
                Intent intent = new Intent(this, LicenseCaptureActivity.class);
                intent.putExtra(EXTRA_SELFIE_URI, selfieUri.toString());
                intent.putExtras(getIntent());
                licenseLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Please capture a selfie", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureLauncher.launch(takePictureIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }
}