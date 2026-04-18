package com.example.carrentingtest.network;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public interface BackendCallback {
    void onSuccess(@NonNull JSONObject response);

    void onError(@NonNull String errorMessage);
}
