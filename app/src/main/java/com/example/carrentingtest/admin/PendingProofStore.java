// java
package com.example.carrentingtest.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PendingProofStore {
    private static final String PREFS = "pending_proofs_prefs";
    private static final String KEY = "pending_payment_proofs";

    public static class PendingProof {
        public String requestId;
        public String filePath;
        public String mime;
        public boolean fromCamera;
        public long timestamp;

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("requestId", requestId);
            o.put("filePath", filePath);
            o.put("mime", mime);
            o.put("fromCamera", fromCamera);
            o.put("timestamp", timestamp);
            return o;
        }

        @Nullable
        public static PendingProof fromJson(JSONObject o) {
            try {
                PendingProof p = new PendingProof();
                p.requestId = o.optString("requestId");
                p.filePath = o.optString("filePath");
                p.mime = o.optString("mime");
                p.fromCamera = o.optBoolean("fromCamera", false);
                p.timestamp = o.optLong("timestamp", System.currentTimeMillis());
                if (TextUtils.isEmpty(p.requestId) || TextUtils.isEmpty(p.filePath)) return null;
                return p;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void savePending(Context ctx, PendingProof p) {
        try {
            JSONArray arr = getArray(ctx);
            arr.put(p.toJson());
            prefs(ctx).edit().putString(KEY, arr.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public static synchronized List<PendingProof> listAll(Context ctx) {
        List<PendingProof> out = new ArrayList<>();
        JSONArray arr = getArray(ctx);
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                PendingProof p = PendingProof.fromJson(o);
                if (p != null) out.add(p);
            } catch (JSONException ignored) {}
        }
        return out;
    }

    public static synchronized void remove(Context ctx, String filePath) {
        JSONArray arr = getArray(ctx);
        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                if (!filePath.equals(o.optString("filePath"))) {
                    out.put(o);
                }
            } catch (JSONException ignored) {}
        }
        prefs(ctx).edit().putString(KEY, out.toString()).apply();
    }

    private static JSONArray getArray(Context ctx) {
        String raw = prefs(ctx).getString(KEY, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static synchronized void clearAll(Context ctx) {
        prefs(ctx).edit().remove(KEY).apply();
    }
}