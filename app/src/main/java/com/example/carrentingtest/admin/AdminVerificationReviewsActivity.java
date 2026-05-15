package com.example.carrentingtest.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.carrentingtest.R;
import com.example.carrentingtest.verification.VerificationStatus;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminVerificationReviewsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private LinearLayout reviewsContainer;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private String companyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_verification_reviews);

        db = FirebaseFirestore.getInstance();
        reviewsContainer = findViewById(R.id.reviewsContainer);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        AdminAccessManager.guardOperationalAccess(this, db, access -> {
            companyId = access.getCompanyId();
            loadReviews();
        });
    }

    private void loadReviews() {
        if (TextUtils.isEmpty(companyId)) return;
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        reviewsContainer.removeAllViews();

        db.collection("verification_requests")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("status", VerificationStatus.UNDER_REVIEW.getStorageValue())
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(snapshot.isEmpty() ? View.VISIBLE : View.GONE);
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        reviewsContainer.addView(createReviewCard(doc));
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(this, R.string.admin_verification_reviews_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private View createReviewCard(@NonNull DocumentSnapshot doc) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getColor(R.color.colorSurface));
        card.setCardElevation(dp(1));
        card.setRadius(dp(18));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.homeCardStroke));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(content);

        TextView title = new TextView(this);
        title.setText(safe(doc.getString("userName"), getString(R.string.admin_verification_unknown_user)));
        title.setTextColor(getColor(R.color.textColorPrimary));
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(title);

        TextView meta = new TextView(this);
        meta.setText(getString(
                R.string.admin_verification_review_meta,
                safe(doc.getString("userEmail"), "-"),
                safe(doc.getString("driverLicense"), "-")));
        meta.setTextColor(getColor(R.color.textColorSecondary));
        meta.setTextSize(13);
        meta.setPadding(0, dp(4), 0, dp(12));
        content.addView(meta);

        TextView liveness = new TextView(this);
        Boolean livenessPassed = doc.getBoolean("livenessPassed");
        liveness.setText(getString(
                R.string.admin_verification_liveness_meta,
                formatLivenessAction(doc.getString("livenessAction")),
                Boolean.TRUE.equals(livenessPassed)
                        ? getString(R.string.admin_verification_liveness_passed)
                        : getString(R.string.admin_verification_liveness_failed)));
        liveness.setTextColor(getColor(Boolean.TRUE.equals(livenessPassed)
                ? R.color.colorSuccess
                : R.color.colorError));
        liveness.setTextSize(13);
        liveness.setPadding(0, 0, 0, dp(12));
        content.addView(liveness);

        LinearLayout images = new LinearLayout(this);
        images.setOrientation(LinearLayout.HORIZONTAL);
        images.setBaselineAligned(false);
        content.addView(images);

        ImageView licenseImage = createEvidenceImage();
        ImageView selfieImage = createEvidenceImage();
        images.addView(licenseImage);
        images.addView(selfieImage);

        loadEvidenceImage(doc, "license_front", licenseImage);
        loadEvidenceImage(doc, "selfie", selfieImage);

        TextView message = new TextView(this);
        message.setText(safe(doc.getString("verificationMessage"), getString(R.string.admin_verification_review_message_fallback)));
        message.setTextColor(getColor(R.color.textColorSecondary));
        message.setTextSize(13);
        message.setPadding(0, dp(12), 0, dp(12));
        content.addView(message);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setBaselineAligned(false);
        content.addView(actions);

        MaterialButton reject = new MaterialButton(this);
        reject.setText(R.string.reject);
        reject.setTextColor(getColor(R.color.colorError));
        reject.setStrokeColorResource(R.color.colorError);
        reject.setStrokeWidth(dp(1));
        reject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        reject.setOnClickListener(v -> decide(doc, VerificationStatus.REJECTED));
        actions.addView(reject, actionParams(true));

        MaterialButton approve = new MaterialButton(this);
        approve.setText(R.string.approve);
        approve.setTextColor(getColor(R.color.white));
        approve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.colorPrimary)));
        approve.setOnClickListener(v -> decide(doc, VerificationStatus.APPROVED));
        actions.addView(approve, actionParams(false));

        return card;
    }

    private ImageView createEvidenceImage() {
        ImageView image = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(150), 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        image.setLayoutParams(params);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundResource(R.drawable.bg_car_image_premium);
        image.setImageResource(R.drawable.ic_person_placeholder);
        return image;
    }

    private LinearLayout.LayoutParams actionParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1);
        params.setMargins(first ? 0 : dp(6), 0, first ? dp(6) : 0, 0);
        return params;
    }

    private void loadEvidenceImage(@NonNull DocumentSnapshot requestDoc,
                                   @NonNull String key,
                                   @NonNull ImageView image) {
        String fieldName = "selfie".equals(key) ? "selfieUrl" : "licenseFrontUrl";
        String url = requestDoc.getString(fieldName);
        if (TextUtils.isEmpty(url)) {
            image.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .into(image);
    }

    private String formatLivenessAction(String value) {
        if (TextUtils.isEmpty(value)) return "-";
        return value.replace('_', ' ');
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private void decide(@NonNull DocumentSnapshot doc, @NonNull VerificationStatus status) {
        String userId = doc.getString("userId");
        if (TextUtils.isEmpty(userId)) return;

        Map<String, Object> requestUpdate = new HashMap<>();
        requestUpdate.put("status", status.getStorageValue());
        requestUpdate.put("manualReviewRequired", false);
        requestUpdate.put("reviewedAt", FieldValue.serverTimestamp());

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("verification_status", status.getStorageValue());
        userUpdate.put("verification_updated_at", FieldValue.serverTimestamp());

        progressBar.setVisibility(View.VISIBLE);
        Tasks.whenAll(
                        doc.getReference().update(requestUpdate),
                        db.collection("users").document(userId).update(userUpdate))
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.admin_verification_review_saved, Toast.LENGTH_SHORT).show();
                    loadReviews();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.admin_verification_review_save_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
