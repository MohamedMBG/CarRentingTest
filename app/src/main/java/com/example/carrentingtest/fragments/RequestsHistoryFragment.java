package com.example.carrentingtest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.ClientRentalRequestAdapter;
import com.example.carrentingtest.models.RentalRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class RequestsHistoryFragment extends Fragment {

    private static final String TAG = "RequestsHistoryFragment";
    private RecyclerView requestsRecyclerView;
    private ClientRentalRequestAdapter adapter;
    private List<RentalRequest> requestList;
    private TextView tvNoRequests;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration historyRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_requests_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        requestsRecyclerView = view.findViewById(R.id.requestsRecyclerView);
        tvNoRequests = view.findViewById(R.id.tvNoRequests);

        // Setup RecyclerView
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestList = new ArrayList<>();
        adapter = new ClientRentalRequestAdapter(requireContext(), requestList);
        adapter.setOnReportClickListener(this::showReportDialog);
        requestsRecyclerView.setAdapter(adapter);

        // Fetch rental history
        fetchRentalHistory();
    }
    private void fetchRentalHistory() {
        // Get the currently logged-in user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;  // Exit if no user is logged in

        // Query Firestore for rental requests:
        // 1. Filter by current user's ID
        // 2. Sort by start date (newest first)
        if (historyRegistration != null) {
            historyRegistration.remove();
        }

        historyRegistration = db.collection("rental_requests")
                .whereEqualTo("userId", user.getUid())
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) {
                        return;
                    }

                    requestList.clear();

                    if (error != null) {
                        Toast.makeText(requireContext(), R.string.no_requests_found, Toast.LENGTH_SHORT).show();
                    } else if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            RentalRequest req = doc.toObject(RentalRequest.class);
                            if (req == null) {
                                continue;
                            }
                            req.setRequestId(doc.getId());
                            requestList.add(req);
                        }
                    }

                    boolean empty = requestList.isEmpty();
                    requestsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    tvNoRequests.setVisibility(empty ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    private void showReportDialog(RentalRequest request) {
        if (!isAdded()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_issue, null, false);
        TextInputEditText input = dialogView.findViewById(R.id.etReportDescription);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.report_issue_title)
                .setMessage(R.string.report_issue_message)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String description = input.getText() != null ? input.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(description)) {
                        Toast.makeText(requireContext(), R.string.report_issue_validation, Toast.LENGTH_SHORT).show();
                        } else {
                        submitReport(request, description);
                    }
                })
                .show();
    }

    private void submitReport(RentalRequest request, String description) {
        Map<String, Object> report = new HashMap<>();
        report.put("requestId", request.getRequestId());
        report.put("carId", request.getCarId());
        report.put("carModel", request.getCarModel());
        report.put("userId", request.getUserId());
        report.put("userName", request.getUserName());
        report.put("userPhone", request.getUserPhone());
        report.put("companyId", request.getCompanyId());
        report.put("description", description);
        report.put("status", "open");
        report.put("startDate", request.getStartDate());
        report.put("endDate", request.getEndDate());
        report.put("createdAt", FieldValue.serverTimestamp());

        db.collection("rental_reports")
                .add(report)
                .addOnSuccessListener(doc -> Toast.makeText(requireContext(), R.string.report_issue_success, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), R.string.report_issue_error, Toast.LENGTH_SHORT).show());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (historyRegistration != null) {
            historyRegistration.remove();
            historyRegistration = null;
        }
    }
}
