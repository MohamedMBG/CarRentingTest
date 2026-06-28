package com.example.carrentingtest.privacy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;

/**
 * Static disclosure of third-party SDKs used by the app and what each one is for.
 *
 * <p>Required by GDPR art. 13/14 (information to the data subject) and App Store /
 * Play Store privacy labelling. Listed entries must stay in sync with the SDKs
 * declared in {@code app/build.gradle.kts}.</p>
 */
public class SdkDisclosureActivity extends AppCompatActivity {

    private static final int[][] SDK_ROWS = new int[][]{
            {R.string.sdk_firebase_auth_title, R.string.sdk_firebase_auth_purpose},
            {R.string.sdk_firestore_title, R.string.sdk_firestore_purpose},
            {R.string.sdk_storage_title, R.string.sdk_storage_purpose},
            {R.string.sdk_analytics_title, R.string.sdk_analytics_purpose},
            {R.string.sdk_fcm_title, R.string.sdk_fcm_purpose},
            {R.string.sdk_config_title, R.string.sdk_config_purpose},
            {R.string.sdk_mlkit_title, R.string.sdk_mlkit_purpose},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdk_disclosure);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.sdk_disclosure_title);
        }

        LinearLayout list = findViewById(R.id.sdkList);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int[] row : SDK_ROWS) {
            list.addView(buildRow(inflater, list, row[0], row[1]));
        }
    }

    private View buildRow(LayoutInflater inflater, LinearLayout parent,
                          @StringRes int titleRes, @StringRes int bodyRes) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padPx = (int) (12 * getResources().getDisplayMetrics().density);
        container.setPadding(0, padPx, 0, padPx);

        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        container.addView(title);

        TextView body = new TextView(this);
        body.setText(bodyRes);
        body.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        container.addView(body);

        return container;
    }
}
