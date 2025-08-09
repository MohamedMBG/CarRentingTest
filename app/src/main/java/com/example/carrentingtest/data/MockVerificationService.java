package com.example.carrentingtest.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public class MockVerificationService implements VerificationService {
    private final boolean autoApprove;

    public MockVerificationService(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    @Override
    public void submitVerification(@NonNull String uid, @NonNull Callback callback) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (autoApprove) {
                callback.onSuccess();
            } else {
                callback.onFailure("mock_reject");
            }
        }, 2000);
    }
}


