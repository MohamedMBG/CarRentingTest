package com.example.carrentingtest.utils;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;

public final class RemoteConfigUtils {
    private static final String KEY_ENABLE_VERIFICATION_GATE = "enable_verification_gate";
    private static final String KEY_VERIFICATION_REQUEST_TIMING = "verification_request_timing";

    private static FirebaseRemoteConfig remoteConfig;

    private RemoteConfigUtils() {}

    public static FirebaseRemoteConfig get() {
        if (remoteConfig == null) {
            remoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build();
            remoteConfig.setConfigSettingsAsync(settings);
            Map<String, Object> defaults = new HashMap<>();
            defaults.put(KEY_ENABLE_VERIFICATION_GATE, true);
            defaults.put(KEY_VERIFICATION_REQUEST_TIMING, "at_booking");
            remoteConfig.setDefaultsAsync(defaults);
            remoteConfig.fetchAndActivate();
        }
        return remoteConfig;
    }

    public static boolean isVerificationGateEnabled() {
        return get().getBoolean(KEY_ENABLE_VERIFICATION_GATE);
    }

    public static String getVerificationRequestTiming() {
        return get().getString(KEY_VERIFICATION_REQUEST_TIMING);
    }
}


