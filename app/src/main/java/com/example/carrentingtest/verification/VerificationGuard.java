package com.example.carrentingtest.verification;

import androidx.annotation.Nullable;

public final class VerificationGuard {
    private VerificationGuard() {}

    public static boolean canBook(@Nullable String verificationStatus) {
        return "VERIFIED".equals(verificationStatus);
    }
}


