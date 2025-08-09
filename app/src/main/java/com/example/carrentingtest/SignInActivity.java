package com.example.carrentingtest;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.admin.AdminDashboardActivity;
import com.example.carrentingtest.admin.AdminLoginActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private View progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);

        // Set click listener for sign in button
        findViewById(R.id.btnSignIn).setOnClickListener(v -> signInUser());
    }

    private void signInUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            db.collection("users").document(user.getUid()).get()
                                    .addOnSuccessListener(doc -> {
                                        String role = doc.getString("role");
                                         Intent intent;
                                         if ("admin".equals(role)) {
                                             intent = new Intent(SignInActivity.this, AdminDashboardActivity.class);
                                         } else {
                                             intent = new Intent(SignInActivity.this, MainActivity.class);
                                         }
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(SignInActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        // Sign in failed
                        Toast.makeText(SignInActivity.this,
                                "Sign in failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Open SignUp Activity
    public void openSignUp(View view) {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    // Open admin Activity
    public void adminSide(View view) {
        startActivity(new Intent(this, AdminLoginActivity.class));
    }


}