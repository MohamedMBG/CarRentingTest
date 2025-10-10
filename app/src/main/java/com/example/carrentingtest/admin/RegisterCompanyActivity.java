package com.example.carrentingtest.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterCompanyActivity extends AppCompatActivity {
    private TextInputLayout tilCompanyName, tilCompanyPhone, tilCompanyAddress, tilAdminName, tilAdminEmail,
            tilAdminPassword, tilConfirmPassword;
    private TextInputEditText etCompanyName, etCompanyPhone, etCompanyAddress, etAdminName, etAdminEmail,
            etAdminPassword, etConfirmPassword;
    private MaterialButton btnRegisterCompany;
    private View progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String companyName;
    private String companyPhone;
    private String companyAddress;
    private String adminName;
    private String email;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_company);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilCompanyName = findViewById(R.id.tilCompanyName);
        tilCompanyPhone = findViewById(R.id.tilCompanyPhone);
        tilCompanyAddress = findViewById(R.id.tilCompanyAddress);
        tilAdminName = findViewById(R.id.tilAdminName);
        tilAdminEmail = findViewById(R.id.tilAdminEmail);
        tilAdminPassword = findViewById(R.id.tilAdminPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etCompanyName = findViewById(R.id.etCompanyName);
        etCompanyPhone = findViewById(R.id.etCompanyPhone);
        etCompanyAddress = findViewById(R.id.etCompanyAddress);
        etAdminName = findViewById(R.id.etAdminName);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
        btnRegisterCompany = findViewById(R.id.btnRegisterCompany);

        btnRegisterCompany.setOnClickListener(v -> {
            clearErrors();
            if (validateInputs()) {
                registerCompany();
            }
        });
    }

    private void registerCompany() {
        toggleLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                String companyId = db.collection("companies").document().getId();

                Map<String, Object> company = new HashMap<>();
                company.put("name", companyName);
                company.put("phone", companyPhone);
                company.put("address", companyAddress);
                company.put("primaryContactName", adminName);
                company.put("primaryContactEmail", email);
                company.put("status", "pending_review");
                company.put("createdAt", FieldValue.serverTimestamp());
                db.collection("companies").document(companyId).set(company)
                        .addOnSuccessListener(aVoid -> saveAdminUser(companyId, email))
                        .addOnFailureListener(e -> showError(e != null && e.getMessage() != null
                                ? e.getMessage()
                                : getString(R.string.error_registration_failed)));
            } else {
                showError(task.getException() != null && task.getException().getMessage() != null
                        ? task.getException().getMessage()
                        : getString(R.string.error_registration_failed));
            }
        }).addOnFailureListener(e -> showError(e != null && e.getMessage() != null
                ? e.getMessage()
                : getString(R.string.error_registration_failed)));
    }

    private void saveAdminUser(String companyId, String email) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("role", "admin");
        user.put("companyId", companyId);
        user.put("displayName", adminName);
        user.put("status", "active");
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    toggleLoading(false);
                    Toast.makeText(this, getString(R.string.business_signup_success), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> showError(e != null && e.getMessage() != null
                        ? e.getMessage()
                        : getString(R.string.error_registration_failed)));
    }

    private void showError(String message) {
        toggleLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private boolean validateInputs() {
        companyName = getText(etCompanyName);
        companyPhone = getText(etCompanyPhone);
        companyAddress = getText(etCompanyAddress);
        adminName = getText(etAdminName);
        email = getText(etAdminEmail);
        password = getText(etAdminPassword);
        String confirmPassword = getText(etConfirmPassword);

        boolean isValid = true;

        if (TextUtils.isEmpty(companyName)) {
            tilCompanyName.setError(getString(R.string.error_company_name_required));
            isValid = false;
        }

        if (!isValidPhone(companyPhone)) {
            tilCompanyPhone.setError(getString(R.string.error_company_phone_invalid));
            isValid = false;
        }

        if (TextUtils.isEmpty(companyAddress) || companyAddress.length() < 6) {
            tilCompanyAddress.setError(getString(R.string.error_company_address_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(adminName)) {
            tilAdminName.setError(getString(R.string.error_admin_name_required));
            isValid = false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilAdminEmail.setError(getString(R.string.error_email_invalid));
            isValid = false;
        }

        if (!isValidPassword(password)) {
            tilAdminPassword.setError(getString(R.string.error_password_requirements));
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        TextInputLayout[] layouts = new TextInputLayout[]{
                tilCompanyName, tilCompanyPhone, tilCompanyAddress, tilAdminName,
                tilAdminEmail, tilAdminPassword, tilConfirmPassword
        };
        for (TextInputLayout layout : layouts) {
            if (layout != null) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        }
    }

    private boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return false;
        }
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 7;
    }

    private boolean isValidPassword(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        return value.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void toggleLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegisterCompany.setEnabled(!loading);
    }
}