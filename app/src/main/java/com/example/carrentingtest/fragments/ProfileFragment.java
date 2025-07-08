package com.example.carrentingtest.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.carrentingtest.MainActivity;
import com.example.carrentingtest.R;
import com.example.carrentingtest.SignInActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    // UI Components
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileLicense;

    // Firebase instances (authentication and database)
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Create the fragment view
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout XML file
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize all text views from layout
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);
        tvProfileLicense = view.findViewById(R.id.tvProfileLicense);

        // Set click listener for logout button
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // Load user profile data
        loadProfile();

        return view;
    }

    // Load user profile from Firestore
    private void loadProfile() {
        // Get currently logged in user
        FirebaseUser user = mAuth.getCurrentUser();

        // If no user, redirect to login screen
        if (user == null) {
            redirectToLogin();
            return;
        }

        // Get user document from Firestore
        db.collection("clients").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    // If failed or document doesn't exist, show default values
                    if (!task.isSuccessful() || !task.getResult().exists()) {
                        setDefaultValues(user);
                        return;
                    }

                    // Get the document snapshot
                    DocumentSnapshot doc = task.getResult();

                    // Update UI with user data
                    tvProfileName.setText(doc.getString("name"));
                    tvProfileEmail.setText(doc.getString("email"));
                    tvProfilePhone.setText(doc.getString("phone"));
                    tvProfileLicense.setText(doc.getString("driverLicense"));
                });
    }

    // Set default values when profile data isn't available
    private void setDefaultValues(FirebaseUser user) {
        tvProfileName.setText("N/A");  // Default name
        tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A"); // Use auth email if available
        tvProfilePhone.setText("N/A");  // Default phone
        tvProfileLicense.setText("N/A"); // Default license
    }

    // Handle logout action
    private void logout() {
        mAuth.signOut();  // Sign out from Firebase

        // Redirect to login screen and clear back stack
        startActivity(new Intent(getActivity(), SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

        requireActivity().finish();  // Close current activity
    }

    // Redirect to login screen
    private void redirectToLogin() {
        // Start login activity
        startActivity(new Intent(getActivity(), SignInActivity.class));

        // Close current activity
        requireActivity().finish();
    }
}