package com.example.carrentingtest.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.carrentingtest.MainActivity;
import com.example.carrentingtest.R;
import com.google.firebase.analytics.FirebaseAnalytics;

public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS = "onboarding_prefs";
    public static final String KEY_SEEN = "onboarding_seen";

    private FirebaseAnalytics analytics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        analytics = FirebaseAnalytics.getInstance(this);

        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new OnboardingPagerAdapter(this));

        findViewById(R.id.btnContinue).setOnClickListener(this::continueOnboarding);
        findViewById(R.id.btnSkip).setOnClickListener(v -> {
            analytics.logEvent("onboarding_skipped", null);
            markSeenAndFinish();
        });

        analytics.logEvent("onboarding_seen", null);
    }

    private void continueOnboarding(View v) {
        ViewPager2 pager = findViewById(R.id.pager);
        if (pager.getCurrentItem() < 2) {
            pager.setCurrentItem(pager.getCurrentItem() + 1, true);
        } else {
            markSeenAndFinish();
        }
    }

    private void markSeenAndFinish() {
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_SEEN, true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}


