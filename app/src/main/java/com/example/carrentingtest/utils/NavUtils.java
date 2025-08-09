package com.example.carrentingtest.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.carrentingtest.MainActivity;

public final class NavUtils {
    private NavUtils() {}

    public static void openProfileForVerification(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("open_profile_for_verification", true);
        activity.startActivity(intent);
    }
}


