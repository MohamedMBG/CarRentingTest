package com.example.carrentingtest.verification.data;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Random;

public enum LivenessAction {
    TURN_LEFT("turn_left"),
    TURN_RIGHT("turn_right"),
    SMILE("smile");

    private static final Random RANDOM = new Random();
    private final String storageValue;

    LivenessAction(String storageValue) {
        this.storageValue = storageValue;
    }

    public String getStorageValue() {
        return storageValue;
    }

    @NonNull
    public static LivenessAction random() {
        LivenessAction[] values = values();
        return values[RANDOM.nextInt(values.length)];
    }

    @NonNull
    public static LivenessAction from(@NonNull String value) {
        String normalized = value.trim().toLowerCase(Locale.US);
        for (LivenessAction action : values()) {
            if (action.storageValue.equals(normalized) || action.name().toLowerCase(Locale.US).equals(normalized)) {
                return action;
            }
        }
        return SMILE;
    }
}
