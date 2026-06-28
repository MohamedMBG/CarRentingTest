package com.example.carrentingtest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carrentingtest.privacy.AgeGate;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {
    //declarations des elements de UI
    private TextInputEditText etEmail, etPassword, etName, etPhone, etDriverLicense, etCompanyId, etDob;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private View progressBar, btnSignUp;

    private String name;
    private String email;
    private String password;
    private String phone;
    private String driverLicense;
    private String companyId;
    private String dob;

    // Selfie/ID verification removed from signup. Registration proceeds directly.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup UI
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etDriverLicense = findViewById(R.id.etDriverLicense);
        etDob = findViewById(R.id.etDob);
        etCompanyId = findViewById(R.id.etCompanyId);
        progressBar = findViewById(R.id.progressBar);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView tvSignIn = findViewById(R.id.tvSignIn);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(v -> registerUser());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                registerUser();
                return true;
            }
            return false;
        });

        // Added onClick listener for Sign In text
        tvSignIn.setOnClickListener(v -> openSignIn());
    }

    private void registerUser() {
        // Get user inputs
        name = Objects.requireNonNull(etName.getText()).toString().trim();
        email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        phone = Objects.requireNonNull(etPhone.getText()).toString().trim();
        driverLicense = Objects.requireNonNull(etDriverLicense.getText()).toString().trim();
        companyId = Objects.requireNonNull(etCompanyId.getText()).toString().trim();
        dob = Objects.requireNonNull(etDob.getText()).toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter your name");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Enter your phone number");
            return;
        }
        if (TextUtils.isEmpty(driverLicense)) {
            etDriverLicense.setError("Enter your driver license number");
            return;
        }
        if (TextUtils.isEmpty(dob)) {
            etDob.setError(getString(R.string.signup_error_dob_required));
            return;
        }
        LocalDate parsedDob = AgeGate.parseIsoDob(dob);
        if (parsedDob == null) {
            etDob.setError(getString(R.string.signup_error_dob_format));
            return;
        }
        if (!AgeGate.isAtLeastMinimumAge(parsedDob)) {
            // Hard block — rental contracts require legal-adult status.
            etDob.setError(getString(R.string.signup_error_dob_age));
            return;
        }
        if (TextUtils.isEmpty(companyId)) {
            etCompanyId.setError(getString(R.string.error_company_id_required));
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password too short (min 6 characters)");
            return;
        }

        setLoading(true);
        validateCompanyAndCreateAccount();
    }

    private void validateCompanyAndCreateAccount() {
        db.collection("companies")
                .document(companyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        createAccount();
                    } else {
                        setLoading(false);
                        etCompanyId.setError(getString(R.string.error_company_not_found_signup));
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_registration_failed), Toast.LENGTH_LONG).show();
                });
    }

    private void createAccount() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveClientData(user.getUid(), name, email, phone, driverLicense, companyId);
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Sign up failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveClientData(String userId, String name, String email, String phone, String driverLicense, String companyId) {
        Map<String, Object> client = new HashMap<>();
        client.put("name", name);
        client.put("email", email);
        client.put("phone", phone);
        client.put("driverLicense", driverLicense);
        // Store DOB ISO-8601 — backend uses this as the legal age-gate record of truth.
        client.put("dob", dob);
        if (companyId != null && !companyId.isEmpty()) {
            client.put("companyId", companyId);
        }
        client.put("role", "client");
        client.put("createdAt", FieldValue.serverTimestamp());
        // Verification defaults
        client.put("verification_status", com.example.carrentingtest.verification.VerificationStatus.NOT_STARTED.getStorageValue());
        client.put("verification_updated_at", null);

        db.collection("users").document(userId)
                .set(client)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity(); // Close all previous activities
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Failed to save user data: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignUp.setEnabled(!loading);
        etName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPhone.setEnabled(!loading);
        etDriverLicense.setEnabled(!loading);
        etDob.setEnabled(!loading);
        etCompanyId.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    // Method to handle Sign In click
    public void openSignIn() {
        startActivity(new Intent(this, SignInActivity.class));
    }

    public void openSignIn(View view) {
        openSignIn();
    }
}

