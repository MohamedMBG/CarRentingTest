package com.example.carrentingtest.privacy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.BuildConfig;
import com.example.carrentingtest.R;
import com.example.carrentingtest.SignInActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * First-launch GDPR consent screen.
 *
 * <p>Granular: analytics and marketing are separate switches. Age confirmation (18+)
 * and ToS / Privacy acceptance are required to continue — anything that gates rental
 * eligibility cannot be a soft opt-in.</p>
 *
 * <p>The screen is reused from the Privacy Center to change consents post-onboarding;
 * in that case {@link #EXTRA_FROM_SETTINGS} skips routing to SignIn and just finishes
 * back to the caller.</p>
 */
public class ConsentActivity extends AppCompatActivity {

    public static final String EXTRA_FROM_SETTINGS = "extra_from_settings";

    private MaterialCheckBox cbAge;
    private MaterialSwitch swAnalytics;
    private MaterialSwitch swMarketing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);

        cbAge = findViewById(R.id.cbAge);
        swAnalytics = findViewById(R.id.swAnalytics);
        swMarketing = findViewById(R.id.swMarketing);

        boolean fromSettings = getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS, false);
        if (fromSettings && ConsentManager.isResolved(this)) {
            // Pre-fill the user's existing choices when editing.
            cbAge.setChecked(ConsentManager.isAgeConfirmed(this));
            swAnalytics.setChecked(ConsentManager.isAnalyticsAllowed(this));
            swMarketing.setChecked(ConsentManager.isMarketingAllowed(this));
        }

        findViewById(R.id.btnViewTerms).setOnClickListener(v ->
                PolicyWebViewActivity.start(this, BuildConfig.TOS_URL, getString(R.string.policy_title_terms)));
        findViewById(R.id.btnViewPrivacy).setOnClickListener(v ->
                PolicyWebViewActivity.start(this, BuildConfig.PRIVACY_POLICY_URL, getString(R.string.policy_title_privacy)));
        findViewById(R.id.btnViewSdks).setOnClickListener(v ->
                startActivity(new Intent(this, SdkDisclosureActivity.class)));

        MaterialButton btnContinue = findViewById(R.id.btnConsentContinue);
        btnContinue.setOnClickListener(v -> onContinue(fromSettings));
    }

    private void onContinue(boolean fromSettings) {
        if (!cbAge.isChecked()) {
            Toast.makeText(this, R.string.consent_error_age_required, Toast.LENGTH_LONG).show();
            return;
        }
        ConsentManager.saveDecision(
                this,
                swAnalytics.isChecked(),
                swMarketing.isChecked(),
                cbAge.isChecked()
        );
        if (fromSettings) {
            setResult(RESULT_OK);
            finish();
        } else {
            // First-launch path → continue to the regular auth entry point.
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Block back-button dismissal on first launch — consent is mandatory.
        // When invoked from settings the user can bail out without saving changes.
        if (getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS, false)) {
            super.onBackPressed();
        }
    }
}
