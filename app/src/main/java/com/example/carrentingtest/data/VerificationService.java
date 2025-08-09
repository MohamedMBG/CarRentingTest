package com.example.carrentingtest.data;

import androidx.annotation.NonNull;

public interface VerificationService {
    interface Callback {
        void onSuccess();
        void onFailure(@NonNull String reasonCode);
    }

    void submitVerification(@NonNull String uid, @NonNull Callback callback);
}


