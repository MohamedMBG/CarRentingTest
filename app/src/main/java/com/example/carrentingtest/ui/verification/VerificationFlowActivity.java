package com.example.carrentingtest.ui.verification;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.carrentingtest.R;

public class VerificationFlowActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private VerificationViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_flow);

        progressBar = findViewById(R.id.progressBar);
        viewModel = new ViewModelProvider(this).get(VerificationViewModel.class);
        viewModel.getIsSubmitting().observe(this, isSubmitting -> {
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(isSubmitting ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new SelfieCaptureFragment())
                    .commit();
        }
    }

    public void onSelfieCaptured(Uri uri) {
        viewModel.setSelfieUri(uri);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new LicenseCaptureFragment())
                .addToBackStack(null)
                .commit();
    }

    public void onLicenseCaptured(Uri uri) {
        viewModel.setLicenseUri(uri);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new VerificationResultFragment())
                .addToBackStack(null)
                .commit();
    }
}


