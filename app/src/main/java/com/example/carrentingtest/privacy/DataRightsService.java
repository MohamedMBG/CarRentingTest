package com.example.carrentingtest.privacy;

import androidx.annotation.NonNull;

import com.example.carrentingtest.BuildConfig;
import com.example.carrentingtest.network.BackendCallback;
import com.example.carrentingtest.network.BackendClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Thin wrapper around {@link BackendClient} for GDPR data-rights endpoints.
 *
 * <p>Server contract:</p>
 * <ul>
 *   <li>{@code POST /v1/user/export} → enqueues an export job, replies with a download URL
 *       (eventually e-mailed) or job id. Backend must aggregate Firestore + Storage assets.</li>
 *   <li>{@code POST /v1/user/delete} → cascades deletion across Firestore (user doc + owned
 *       sub-collections), Firebase Storage (selfies, license scans, signatures) and Auth.
 *       Backend must run in a privileged context (Admin SDK) — the client cannot delete
 *       its own Auth record from another user's data, and Firestore security rules will
 *       block client-side cascading deletes.</li>
 * </ul>
 *
 * <p>Both calls send the user's Firebase ID token via {@link BackendClient}'s automatic
 * Bearer header so the backend can authenticate the caller.</p>
 */
public final class DataRightsService {

    private DataRightsService() {}

    public static void requestExport(@NonNull BackendCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not signed in.");
            return;
        }
        try {
            JSONObject payload = new JSONObject()
                    .put("uid", user.getUid())
                    .put("email", user.getEmail());
            BackendClient.postJson(BuildConfig.EXPORT_ENDPOINT_PATH, payload, callback);
        } catch (JSONException e) {
            callback.onError("Failed to build export request.");
        }
    }

    public static void requestDeletion(@NonNull BackendCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not signed in.");
            return;
        }
        try {
            JSONObject payload = new JSONObject()
                    .put("uid", user.getUid())
                    .put("email", user.getEmail())
                    // Why: helps backend log the user-confirmed intent for audit.
                    .put("confirmed", true);
            BackendClient.postJson(BuildConfig.DELETE_ENDPOINT_PATH, payload, callback);
        } catch (JSONException e) {
            callback.onError("Failed to build deletion request.");
        }
    }
}
