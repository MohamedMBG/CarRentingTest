package com.example.carrentingtest.utils;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.carrentingtest.ui.verify.VerificationIntroFragment;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public final class VerificationGate {

    private VerificationGate() {}

    public static boolean isVerified(@Nullable FirebaseUser user, @Nullable DocumentSnapshot userDoc) {
        if (user == null || userDoc == null || !userDoc.exists()) return false;
        String status = userDoc.getString("verification_status");
        return status != null && status.equals("verified");
    }

    public static void requireVerified(FragmentActivity activity, Fragment currentFragment) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        VerificationIntroFragment fragment = new VerificationIntroFragment();
        fragment.setArguments(new Bundle());
        tx.replace(currentFragment.getId(), fragment);
        tx.addToBackStack(null);
        tx.commit();
    }
}


