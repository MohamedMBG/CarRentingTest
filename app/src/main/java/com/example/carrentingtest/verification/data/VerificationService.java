package com.example.carrentingtest.verification.data;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

public interface VerificationService {
    LiveData<VerificationResult> submit(@NonNull Uri selfie, @NonNull Uri licenseFront);
}


