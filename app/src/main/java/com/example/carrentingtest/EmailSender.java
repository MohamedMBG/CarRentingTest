package com.example.carrentingtest;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.carrentingtest.network.BackendCallback;
import com.example.carrentingtest.network.BackendClient;

import org.json.JSONObject;

public class EmailSender {
    private static final String TAG = "EmailSender";

    // Callback interface to handle email sending results
    public interface EmailCallback {
        void onSuccess();                   // Called when email sends successfully
        void onFailure(String error);       // Called when email fails to send
    }

    /**
     * Sends an email request through the backend notification endpoint.
     * @param context   Android context (must be Activity for UI thread callbacks)
     * @param recipient Email address of recipient
     * @param subject   Email subject line
     * @param body      Email content
     * @param callback  Callback to handle success/failure
     */
    public static void sendEmail(Context context, String recipient, String subject, String body, EmailCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("recipient", recipient);
            payload.put("subject", subject);
            payload.put("body", body);

            BackendClient.postJson(BuildConfig.NOTIFICATION_ENDPOINT_PATH, payload, new BackendCallback() {
                @Override
                public void onSuccess(@NonNull JSONObject response) {
                    if (callback != null) {
                        runOnUiThread(context, callback::onSuccess);
                    }
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    Log.e(TAG, "Notification proxy request failed: " + errorMessage);
                    if (callback != null) {
                        runOnUiThread(context, () -> callback.onFailure(errorMessage));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare notification payload", e);
            if (callback != null) {
                runOnUiThread(context, () -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to queue email request."));
            }
        }
    }

    private static void runOnUiThread(Context context, Runnable action) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(action);
        } else {
            action.run();
        }
    }
}
