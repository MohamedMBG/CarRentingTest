package com.example.carrentingtest.privacy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.BuildConfig;
import com.example.carrentingtest.R;
import com.example.carrentingtest.SignInActivity;
import com.example.carrentingtest.network.BackendCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

/**
 * Settings hub for the user's privacy choices and GDPR data rights.
 *
 * <p>Surfaces the user's current consent state, lets them open the granular
 * {@link ConsentActivity} to change it, request a data export, request account
 * deletion, and read each policy or SDK list.</p>
 */
public class PrivacyCenterActivity extends AppCompatActivity {

    private TextView tvAnalyticsState;
    private TextView tvMarketingState;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_center);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.privacy_center_title);
        }

        tvAnalyticsState = findViewById(R.id.tvAnalyticsState);
        tvMarketingState = findViewById(R.id.tvMarketingState);
        progress = findViewById(R.id.privacyCenterProgress);

        findViewById(R.id.btnChangeConsent).setOnClickListener(v -> {
            Intent intent = new Intent(this, ConsentActivity.class);
            intent.putExtra(ConsentActivity.EXTRA_FROM_SETTINGS, true);
            startActivity(intent);
        });

        findViewById(R.id.btnExport).setOnClickListener(v -> confirmExport());
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDeletion());

        findViewById(R.id.btnViewPrivacy).setOnClickListener(v ->
                PolicyWebViewActivity.start(this, BuildConfig.PRIVACY_POLICY_URL,
                        getString(R.string.policy_title_privacy)));
        findViewById(R.id.btnViewTerms).setOnClickListener(v ->
                PolicyWebViewActivity.start(this, BuildConfig.TOS_URL,
                        getString(R.string.policy_title_terms)));
        findViewById(R.id.btnViewSdks).setOnClickListener(v ->
                startActivity(new Intent(this, SdkDisclosureActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshConsentState();
    }

    private void refreshConsentState() {
        tvAnalyticsState.setText(ConsentManager.isAnalyticsAllowed(this)
                ? R.string.privacy_center_state_allowed
                : R.string.privacy_center_state_denied);
        tvMarketingState.setText(ConsentManager.isMarketingAllowed(this)
                ? R.string.privacy_center_state_allowed
                : R.string.privacy_center_state_denied);
    }

    private void confirmExport() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.privacy_center_export_confirm_title)
                .setMessage(R.string.privacy_center_export_confirm_body)
                .setPositiveButton(R.string.privacy_center_action_request, (d, w) -> doExport())
                .setNegativeButton(R.string.privacy_center_action_cancel, null)
                .show();
    }

    private void doExport() {
        setBusy(true);
        DataRightsService.requestExport(new BackendCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setBusy(false);
                Toast.makeText(PrivacyCenterActivity.this,
                        R.string.privacy_center_export_started, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                Toast.makeText(PrivacyCenterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeletion() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.privacy_center_delete_confirm_title)
                .setMessage(R.string.privacy_center_delete_confirm_body)
                .setPositiveButton(R.string.privacy_center_action_delete, (d, w) -> doDeletion())
                .setNegativeButton(R.string.privacy_center_action_cancel, null)
                .show();
    }

    private void doDeletion() {
        setBusy(true);
        Toast.makeText(this, R.string.privacy_center_delete_started, Toast.LENGTH_SHORT).show();
        DataRightsService.requestDeletion(new BackendCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // Backend has cascaded Firestore + Storage + Auth deletion. Wipe local
                // session, then route to SignIn so the now-orphaned token isn't reused.
                ConsentManager.clear(PrivacyCenterActivity.this);
                FirebaseAuth.getInstance().signOut();
                setBusy(false);
                Toast.makeText(PrivacyCenterActivity.this,
                        R.string.privacy_center_delete_done, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(PrivacyCenterActivity.this, SignInActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                Toast.makeText(PrivacyCenterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        // Disable destructive actions while a request is in flight.
        findViewById(R.id.btnExport).setEnabled(!busy);
        findViewById(R.id.btnDelete).setEnabled(!busy);
    }
}
