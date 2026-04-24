package com.example.carrentingtest.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.example.carrentingtest.data.repository.UserRepository;
import com.example.carrentingtest.domain.UserRole;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private View progressBar, btnLogin, btnRegisterCompany;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);
        FullscreenUiHelper.apply(this, R.id.admin_login_root);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegisterCompany = findViewById(R.id.tvRegisterCompany);

        btnLogin.setOnClickListener(v -> loginAdmin());
        btnRegisterCompany.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterCompanyActivity.class)));
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginAdmin();
                return true;
            }
            return false;
        });
    }

    private void loginAdmin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.setError("Enter a valid email");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    userRepository.getById(user.getUid())
                            .addOnSuccessListener(doc -> {
                                if (doc == null || !doc.exists()) {
                                    setLoading(false);
                                    mAuth.signOut();
                                    Toast.makeText(this, "No admin profile found", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                UserRole role = UserRole.from(doc.getString("role"));
                                if (role != UserRole.ADMIN) {
                                    setLoading(false);
                                    Toast.makeText(this, "Not an admin account", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                    return;
                                }
                                AdminAccessManager.verifyOperationalAccess(
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                                        user,
                                        new AdminAccessManager.AccessCallback() {
                                    @Override
                                    public void onGranted(@androidx.annotation.NonNull AdminAccessManager.AdminAccess access) {
                                        startActivity(new Intent(AdminLoginActivity.this, AdminDashboardActivity.class));
                                        finish();
                                    }

                                    @Override
                                    public void onDenied(@androidx.annotation.NonNull String message) {
                                        mAuth.signOut();
                                        setLoading(false);
                                        Toast.makeText(AdminLoginActivity.this, message, Toast.LENGTH_LONG).show();
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Failed to load user", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    setLoading(false);
                    Toast.makeText(this, "Unable to load signed-in user", Toast.LENGTH_SHORT).show();
                }
            } else {
                setLoading(false);
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegisterCompany.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }
}
