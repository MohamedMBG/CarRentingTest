package com.example.carrentingtest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.admin.AdminAccessManager;
import com.example.carrentingtest.admin.AdminDashboardActivity;
import com.example.carrentingtest.admin.AdminLoginActivity;
import com.example.carrentingtest.data.repository.UserRepository;
import com.example.carrentingtest.domain.UserRole;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private android.widget.CheckBox cbRememberMe;
    private View progressBar, btnSignIn;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private SharedPreferences authPrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        progressBar = findViewById(R.id.progressBar);

        // Restore remembered email
        boolean remembered = authPrefs.getBoolean("remember_me", false);
        cbRememberMe.setChecked(remembered);
        if (remembered) {
            String savedEmail = authPrefs.getString("email", "");
            if (savedEmail != null) etEmail.setText(savedEmail);
        }

        // Set click listener for sign in button
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(v -> signInUser());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                signInUser();
                return true;
            }
            return false;
        });
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

        setLoading(true);

        // Sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save remember me preference
                        authPrefs.edit()
                                .putBoolean("remember_me", cbRememberMe.isChecked())
                                .putString("email", cbRememberMe.isChecked() ? email : "")
                                .apply();
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            userRepository.getById(user.getUid())
                                    .addOnSuccessListener(doc -> {
                                        if (doc == null || !doc.exists()) {
                                            mAuth.signOut();
                                            setLoading(false);
                                            Toast.makeText(SignInActivity.this,
                                                    "No account found for this email. Please sign up first.",
                                                    Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                        UserRole role = UserRole.from(doc.getString("role"));
                                        if (role == UserRole.ADMIN) {
                                            AdminAccessManager.verifyOperationalAccess(
                                                    com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                                                    user,
                                                    new AdminAccessManager.AccessCallback() {
                                                @Override
                                                public void onGranted(@androidx.annotation.NonNull AdminAccessManager.AdminAccess access) {
                                                    startActivity(new Intent(SignInActivity.this, AdminDashboardActivity.class));
                                                    finish();
                                                }

                                                @Override
                                                public void onDenied(@androidx.annotation.NonNull String message) {
                                                    mAuth.signOut();
                                                    setLoading(false);
                                                    Toast.makeText(SignInActivity.this, message, Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        } else {
                                            startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        Toast.makeText(SignInActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            setLoading(false);
                            Toast.makeText(SignInActivity.this, "Unable to load signed-in user", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoading(false);
                        // Sign in failed
                        Toast.makeText(SignInActivity.this,
                                "Sign in failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        cbRememberMe.setEnabled(!loading);
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
