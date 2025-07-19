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

import com.example.carrentingtest.utils.FaceNetUtil;

import java.io.IOException;

public class SelfieCaptureActivity extends AppCompatActivity {
    private static final int REQ_SELFIE = 101;
    private static final float MATCH_THRESHOLD = 0.7f;

    private ImageView ivPreview;
    private Bitmap selfieBitmap;
    private FaceNetUtil faceNetUtil;

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    selfieBitmap = (Bitmap) extras.get("data");
                    if (selfieBitmap != null) {
                        ivPreview.setImageBitmap(selfieBitmap);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> licenseLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_capture);

        faceNetUtil = FaceNetUtil.create(this);
        ivPreview = findViewById(R.id.ivSelfiePreview);
        Button btnCapture = findViewById(R.id.btnCaptureSelfie);
        Button btnNext = findViewById(R.id.btnSelfieNext);

        btnCapture.setOnClickListener(v -> launchCamera());
        // In the btnNext.setOnClickListener, update the intent creation:
        // In the btnNext.setOnClickListener:
        btnNext.setOnClickListener(v -> {
            if (selfieBitmap != null) {
                Intent intent = new Intent(this, LicenseCaptureActivity.class);
                intent.putExtra("selfie_bitmap", selfieBitmap);
                // Pass the driver license number from the original signup intent
                intent.putExtra("driverLicense", getIntent().getStringExtra("driverLicense"));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (selfieBitmap != null && !selfieBitmap.isRecycled()) {
            selfieBitmap.recycle();
        }
    }
}