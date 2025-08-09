package com.example.carrentingtest.verification.data;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class MockVerificationService implements VerificationService {

    private final boolean isDebug;

    public MockVerificationService(Context context) {
        // Simple heuristic: treat debuggable as debug
        this.isDebug = (0 != (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE));
    }

    @Override
    public LiveData<VerificationResult> submit(@NonNull Uri selfie, @NonNull Uri licenseFront) {
        MutableLiveData<VerificationResult> live = new MutableLiveData<>();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            VerificationResult.Status status = isDebug ? VerificationResult.Status.VERIFIED : VerificationResult.Status.PENDING;
            live.setValue(new VerificationResult(status));
        }, 1500);
        return live;
    }
}


