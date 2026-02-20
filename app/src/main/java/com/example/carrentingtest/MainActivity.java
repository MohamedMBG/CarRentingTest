package com.example.carrentingtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;


import com.example.carrentingtest.fragments.HomeFragment;
import com.example.carrentingtest.fragments.ProfileFragment;
import com.example.carrentingtest.fragments.RequestsHistoryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            // Not logged in, redirect to SignInActivity
            startActivity(new Intent(this, SignInActivity.class));
            finish(); // Prevent returning to MainActivity via back button
            return; // Stop further execution in onCreate
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);
        enableFullscreenMode();
        applyEdgeToEdgeInsets();

        // Load the default fragment (HomeFragment) or route to Profile for verification
        if (savedInstanceState == null) {
            boolean openProfileForVerification = getIntent().getBooleanExtra("open_profile_for_verification", false);
            if (openProfileForVerification) {
                loadFragment(new ProfileFragment());
                bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
            } else {
                loadFragment(new HomeFragment());
                bottomNavigationView.setSelectedItemId(R.id.navigation_home); // Set default selection
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.navigation_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.navigation_requests) {
            fragment = new RequestsHistoryFragment();
        } else if (itemId == R.id.navigation_profile) {
            fragment = new ProfileFragment();
        } else if (itemId == R.id.navigation_map) {
            fragment = new com.example.carrentingtest.fragments.AgencyMapFragment();
        }

        if (fragment != null) {
            loadFragment(fragment);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        // transaction.addToBackStack(null); // Optional: Add to back stack if needed
        transaction.commit();
    }

    // Method to handle logout (can be called from ProfileFragment)
    public void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, SignInActivity.class));
        finishAffinity(); // Close all activities in the task
    }

    private void enableFullscreenMode() {
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController == null) {
            return;
        }
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void applyEdgeToEdgeInsets() {
        View root = findViewById(R.id.main_root);
        View fragmentContainer = findViewById(R.id.fragment_container);
        View navContainer = findViewById(R.id.bottom_nav_container);
        ViewGroup.MarginLayoutParams navLp = (ViewGroup.MarginLayoutParams) navContainer.getLayoutParams();
        final int initialNavBottomMargin = navLp.bottomMargin;
        final int fallbackNavClearance = (int) (88 * getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int navHeight = navContainer.getHeight();
            if (navHeight <= 0 && navContainer.getLayoutParams() != null && navContainer.getLayoutParams().height > 0) {
                navHeight = navContainer.getLayoutParams().height;
            }
            int navClearance = navHeight > 0
                    ? navHeight + initialNavBottomMargin + systemBars.bottom
                    : fallbackNavClearance + systemBars.bottom;
            fragmentContainer.setPadding(0, systemBars.top, 0, navClearance);
            navLp.bottomMargin = initialNavBottomMargin + systemBars.bottom;
            navContainer.setLayoutParams(navLp);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        navContainer.post(() -> ViewCompat.requestApplyInsets(root));
    }
}

