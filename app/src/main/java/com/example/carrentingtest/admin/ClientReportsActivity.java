package com.example.carrentingtest.admin;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalReport;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class ClientReportsActivity extends AppCompatActivity implements ClientReportsAdapter.OnCallClientListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private View progressBar;
    private final List<RentalReport> reports = new ArrayList<>();
    private ClientReportsAdapter adapter;
    private FirebaseFirestore db;
    private String companyId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_reports);
        FullscreenUiHelper.apply(this, R.id.client_reports_root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerReports);
        tvEmpty = findViewById(R.id.tvEmptyReports);
        progressBar = findViewById(R.id.progressBar);

        adapter = new ClientReportsAdapter(reports, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        AdminAccessManager.guardOperationalAccess(this, db, access -> {
            companyId = access.getCompanyId();
            loadReports();
        });
    }

    private void loadReports() {
        if (companyId == null) {
            updateEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        db.collection("rental_reports")
                .whereEqualTo("companyId", companyId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null || snapshot == null) {
                        Toast.makeText(this, R.string.client_reports_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reports.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        RentalReport report = doc.toObject(RentalReport.class);
                        if (report == null) {
                            continue;
                        }
                        report.setId(doc.getId());
                        reports.add(report);
                    }
                    adapter.notifyDataSetChanged();
                    resolveMissingPhones();
                    updateEmptyState();
                });
    }

    private void resolveMissingPhones() {
        for (RentalReport report : reports) {
            if (TextUtils.isEmpty(report.getUserPhone()) && !TextUtils.isEmpty(report.getUserId())) {
                db.collection("users")
                        .document(report.getUserId())
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String phone = userDoc.getString("phone");
                            if (!TextUtils.isEmpty(phone)) {
                                report.setUserPhone(phone);
                                int index = reports.indexOf(report);
                                if (index >= 0) {
                                    adapter.notifyItemChanged(index);
                                } else {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
            }
        }
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCall(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, R.string.no_phone_available, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_phone_available, Toast.LENGTH_SHORT).show();
        }
    }
}
