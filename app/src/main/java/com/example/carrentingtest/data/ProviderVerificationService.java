package com.example.carrentingtest.data;

import androidx.annotation.NonNull;

/**
 * Stub implementation placeholder for a real verification provider.
 */
public class ProviderVerificationService implements VerificationService {
    @Override
    public void submitVerification(@NonNull String uid, @NonNull Callback callback) {
        // TODO: Integrate with real provider SDK/API
        callback.onFailure("not_implemented");
    }
}


