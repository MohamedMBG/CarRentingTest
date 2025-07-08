package com.example.carrentingtest.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterCompanyActivity extends AppCompatActivity {
    private TextInputEditText etCompanyName, etAdminEmail, etAdminPassword;
    private View progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_company);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etCompanyName = findViewById(R.id.etCompanyName);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnRegisterCompany).setOnClickListener(v -> registerCompany());
    }

    private void registerCompany() {
        String companyName = String.valueOf(etCompanyName.getText()).trim();
        String email = String.valueOf(etAdminEmail.getText()).trim();
        String password = String.valueOf(etAdminPassword.getText()).trim();

        if (TextUtils.isEmpty(companyName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                String companyId = db.collection("companies").document().getId();

                Map<String, Object> company = new HashMap<>();
                company.put("name", companyName);
                company.put("createdAt", FieldValue.serverTimestamp());
                db.collection("companies").document(companyId).set(company)
                        .addOnSuccessListener(aVoid -> saveAdminUser(companyId, email))
                        .addOnFailureListener(e -> showError(e.getMessage()));
            } else {
                showError(task.getException() != null ? task.getException().getMessage() : "Registration failed");
            }
        });
    }

    private void saveAdminUser(String companyId, String email) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("role", "admin");
        user.put("companyId", companyId);
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Company registered", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}