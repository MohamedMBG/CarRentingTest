package com.example.carrentingtest.network;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.carrentingtest.BuildConfig;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackendClient {
    private static final int TIMEOUT_MS = 15000;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private BackendClient() {}

    public static boolean isConfigured() {
        return !TextUtils.isEmpty(getBaseUrl());
    }

    public static void postJson(@NonNull String endpointPath,
                                @NonNull JSONObject payload,
                                @NonNull BackendCallback callback) {
        String baseUrl = getBaseUrl();
        if (TextUtils.isEmpty(baseUrl)) {
            postError(callback, "Backend is not configured.");
            return;
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(buildUrl(baseUrl, endpointPath));
                if (BuildConfig.REQUIRE_HTTPS_BACKEND && !"https".equalsIgnoreCase(url.getProtocol())) {
                    throw new IllegalStateException("Backend URL must use HTTPS in release builds.");
                }

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setDoOutput(true);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                String authToken = tryGetAuthToken();
                if (!TextUtils.isEmpty(authToken)) {
                    connection.setRequestProperty("Authorization", "Bearer " + authToken);
                }

                byte[] requestBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestBytes);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();
                boolean success = responseCode >= 200 && responseCode < 300;
                String rawBody = readFully(success ? connection.getInputStream() : connection.getErrorStream());

                if (!success) {
                    String message = !TextUtils.isEmpty(rawBody)
                            ? rawBody
                            : "Backend request failed with status " + responseCode;
                    postError(callback, message);
                    return;
                }

                postSuccess(callback, parseResponse(rawBody));
            } catch (Exception e) {
                postError(callback, e.getMessage() != null ? e.getMessage() : "Backend request failed.");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @NonNull
    private static String buildUrl(@NonNull String baseUrl, @NonNull String endpointPath) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        return normalizedBase + normalizedPath;
    }

    @NonNull
    private static String getBaseUrl() {
        return BuildConfig.BACKEND_BASE_URL == null ? "" : BuildConfig.BACKEND_BASE_URL.trim();
    }

    private static String tryGetAuthToken() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return null;
            }
            return Tasks.await(currentUser.getIdToken(false)).getToken();
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private static JSONObject parseResponse(String rawBody) throws JSONException {
        if (TextUtils.isEmpty(rawBody)) {
            return new JSONObject();
        }
        return new JSONObject(rawBody);
    }

    @NonNull
    private static String readFully(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        try (InputStream stream = new BufferedInputStream(inputStream);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void postSuccess(@NonNull BackendCallback callback, @NonNull JSONObject response) {
        MAIN_HANDLER.post(() -> callback.onSuccess(response));
    }

    private static void postError(@NonNull BackendCallback callback, @NonNull String message) {
        MAIN_HANDLER.post(() -> callback.onError(message));
    }
}
