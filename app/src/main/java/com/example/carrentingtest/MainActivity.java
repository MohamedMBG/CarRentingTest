package com.example.carrentingtest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatDelegate;


import com.example.carrentingtest.fragments.HomeFragment;
import com.example.carrentingtest.fragments.ProfileFragment;
import com.example.carrentingtest.fragments.RequestsHistoryFragment;
import me.ibrahimsn.lib.OnItemSelectedListener;
import me.ibrahimsn.lib.SmoothBottomBar;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private SmoothBottomBar bottomNavigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        bottomNavigationView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelect(int index) {
                Fragment fragment = null;
                if (index == 0) {
                    fragment = new HomeFragment();
                } else if (index == 1) {
                    fragment = new RequestsHistoryFragment();
                } else if (index == 2) {
                    fragment = new ProfileFragment();
                }
                if (fragment != null) {
                    loadFragment(fragment);
                }
            }
        });

        // Load the default fragment (HomeFragment)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.setSelectedItem(0); // Set default selection
        }
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
}

