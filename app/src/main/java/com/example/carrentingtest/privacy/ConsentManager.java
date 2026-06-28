package com.example.carrentingtest.privacy;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Persists the user's GDPR consent decisions and propagates them to SDKs.
 *
 * <p>Two consents are tracked separately (analytics, marketing) so a user can opt
 * into one without the other — required by GDPR (consent must be granular).
 * Terms / Privacy acceptance and age confirmation are tracked alongside.</p>
 */
public final class ConsentManager {

    private static final String PREFS = "privacy_consent_prefs";
    private static final String KEY_RESOLVED = "consent_resolved";
    private static final String KEY_TERMS_ACCEPTED = "terms_accepted";
    private static final String KEY_ANALYTICS = "consent_analytics";
    private static final String KEY_MARKETING = "consent_marketing";
    private static final String KEY_AGE_CONFIRMED = "age_confirmed_18";
    private static final String KEY_TIMESTAMP = "consent_timestamp";
    private static final String KEY_POLICY_VERSION = "policy_version_accepted";

    /**
     * Bump when the privacy policy / terms text materially changes. Stored value mismatch
     * forces re-consent (see {@link #isCurrentPolicyAccepted(Context)}).
     */
    public static final int CURRENT_POLICY_VERSION = 1;

    private ConsentManager() {}

    public static boolean isResolved(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_RESOLVED, false);
    }

    public static boolean isCurrentPolicyAccepted(@NonNull Context context) {
        return isResolved(context)
                && prefs(context).getInt(KEY_POLICY_VERSION, 0) >= CURRENT_POLICY_VERSION;
    }

    public static boolean isAnalyticsAllowed(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_ANALYTICS, false);
    }

    public static boolean isMarketingAllowed(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_MARKETING, false);
    }

    public static boolean isAgeConfirmed(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_AGE_CONFIRMED, false);
    }

    public static boolean areTermsAccepted(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_TERMS_ACCEPTED, false);
    }

    /**
     * Persist an explicit consent decision and apply it to SDKs that respect a runtime
     * collection toggle. ToS acceptance and age confirmation are required to reach this
     * call; they are not opt-in toggles.
     */
    public static void saveDecision(@NonNull Context context,
                                    boolean analyticsAllowed,
                                    boolean marketingAllowed,
                                    boolean ageConfirmed) {
        prefs(context).edit()
                .putBoolean(KEY_RESOLVED, true)
                .putBoolean(KEY_TERMS_ACCEPTED, true)
                .putBoolean(KEY_ANALYTICS, analyticsAllowed)
                .putBoolean(KEY_MARKETING, marketingAllowed)
                .putBoolean(KEY_AGE_CONFIRMED, ageConfirmed)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .putInt(KEY_POLICY_VERSION, CURRENT_POLICY_VERSION)
                .apply();
        applyAnalyticsConsent(context, analyticsAllowed);
    }

    /**
     * Wipe stored consent — used by the deletion flow before signing out so the next
     * launch re-prompts (and so old decisions don't leak between users on shared devices).
     */
    public static void clear(@NonNull Context context) {
        prefs(context).edit().clear().apply();
        applyAnalyticsConsent(context, false);
    }

    /**
     * Re-apply the persisted analytics consent to {@link FirebaseAnalytics}. Call from
     * app startup so the SDK matches the user's choice on every cold start. If consent
     * has never been resolved we default to disabled (GDPR — no consent = no processing).
     */
    public static void applyPersistedAnalyticsConsent(@NonNull Context context) {
        boolean allowed = isResolved(context) && isAnalyticsAllowed(context);
        applyAnalyticsConsent(context, allowed);
    }

    private static void applyAnalyticsConsent(@NonNull Context context, boolean allowed) {
        try {
            FirebaseAnalytics.getInstance(context.getApplicationContext())
                    .setAnalyticsCollectionEnabled(allowed);
        } catch (Exception ignored) {
            // Firebase not initialised yet on very early startup; subsequent calls will apply it.
        }
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
