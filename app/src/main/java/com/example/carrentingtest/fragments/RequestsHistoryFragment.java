package com.example.carrentingtest.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.ClientRentalRequestAdapter;
import com.example.carrentingtest.models.RentalRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class RequestsHistoryFragment extends Fragment {

    private static final String TAG = "RequestsHistoryFragment";
    private RecyclerView requestsRecyclerView;
    private ClientRentalRequestAdapter adapter;
    private List<RentalRequest> requestList;
    private TextView tvNoRequests;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
        requestsRecyclerView.setAdapter(adapter);

        // Fetch rental history
        fetchRentalHistory();
    }

    private void fetchRentalHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in.");
            Toast.makeText(getContext(), "Please log in to view history.", Toast.LENGTH_SHORT).show();
            // Optionally redirect to login
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Fetching rental history for user ID: " + userId);

        db.collection("rentalRequests")
                .whereEqualTo("userId", userId)
                .orderBy("startDate", Query.Direction.DESCENDING) // Order by start date, newest first
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        requestList.clear();
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                RentalRequest request = document.toObject(RentalRequest.class);
                                request.setRequestId(document.getId()); // Set the document ID
                                requestList.add(request);
                            }
                            Log.d(TAG, "Successfully fetched " + requestList.size() + " rental requests.");
                            adapter.notifyDataSetChanged();
                            requestsRecyclerView.setVisibility(View.VISIBLE);
                            tvNoRequests.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG, "No rental requests found for this user.");
                            requestsRecyclerView.setVisibility(View.GONE);
                            tvNoRequests.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e(TAG, "Error getting rental requests: ", task.getException());
                        Toast.makeText(getContext(), "Error fetching rental history: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        requestsRecyclerView.setVisibility(View.GONE);
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText("Error loading history."); // Update text on error
                    }
                });
    }
}
