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

    private static final String TAG = "ProfileFragment";
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileLicense;
    private Button btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);
        tvProfileLicense = view.findViewById(R.id.tvProfileLicense);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Load user data
        loadUserProfile();

        // Set logout button click listener
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Log.d(TAG, "User data found: " + document.getData());
                        // Populate TextViews
                        tvProfileName.setText(document.getString("name"));
                        tvProfileEmail.setText(document.getString("email"));
                        tvProfilePhone.setText(document.getString("phone"));
                        tvProfileLicense.setText(document.getString("driverLicense"));
                    } else {
                        Log.d(TAG, "No such user document");
                        Toast.makeText(getContext(), "User profile data not found.", Toast.LENGTH_SHORT).show();
                        // Set default text or handle error
                        tvProfileName.setText("N/A");
                        tvProfileEmail.setText(currentUser.getEmail()); // Use email from Auth if available
                        tvProfilePhone.setText("N/A");
                        tvProfileLicense.setText("N/A");
                    }
                } else {
                    Log.e(TAG, "Error getting user document: ", task.getException());
                    Toast.makeText(getContext(), "Error loading profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    // Set default text or handle error
                    tvProfileName.setText("Error");
                    tvProfileEmail.setText("Error");
                    tvProfilePhone.setText("Error");
                    tvProfileLicense.setText("Error");
                }
            });
        } else {
            // Should not happen if MainActivity checks login status, but handle anyway
            Log.w(TAG, "Current user is null in ProfileFragment");
            Toast.makeText(getContext(), "Not logged in.", Toast.LENGTH_SHORT).show();
            // Optionally redirect to login
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logoutUser(); // Use existing logout logic
            }
        }
    }

    private void logout() {
        // Call the logout method in MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).logoutUser();
        } else {
            // Fallback if not attached to MainActivity (should not happen)
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}