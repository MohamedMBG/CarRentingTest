package com.example.carrentingtest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {
    //declarations des elements de UI
    private TextInputEditText etEmail, etPassword, etName, etPhone, etDriverLicense, etCompanyId;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private View progressBar;

    private String name;
    private String email;
    private String password;
    private String phone;
    private String driverLicense;
    private String companyId;

    private final ActivityResultLauncher<Intent> registrationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    createAccount();
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            });

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
        etCompanyId = findViewById(R.id.etCompanyId);
        progressBar = findViewById(R.id.progressBar);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView tvSignIn = findViewById(R.id.tvSignIn);

        Button btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(v -> registerUser());

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
        if (TextUtils.isEmpty(companyId)) {
            etCompanyId.setError("Enter company ID");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password too short (min 6 characters)");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, SelfieCaptureActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        intent.putExtra("phone", phone);
        intent.putExtra("driverLicense", driverLicense);
        intent.putExtra("companyId", companyId);
        registrationLauncher.launch(intent);
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
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Sign up failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveClientData(String userId, String name, String email, String phone, String driverLicense, String companyId) {
        Map<String, Object> client = new HashMap<>();
        client.put("name", name);
        client.put("email", email);
        client.put("phone", phone);
        client.put("driverLicense", driverLicense); // Add driver license to map
        client.put("companyId", companyId);
        client.put("role", "client");
        client.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(client)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity(); // Close all previous activities
                    } else {
                        Toast.makeText(this, "Failed to save user data: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Method to handle Sign In click
    public void openSignIn() {
        startActivity(new Intent(this, SignInActivity.class));
    }
}

