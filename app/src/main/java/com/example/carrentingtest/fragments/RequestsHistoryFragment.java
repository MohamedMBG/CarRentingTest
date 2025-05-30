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
import com.google.firebase.firestore.FirebaseFirestoreException;
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
        Log.d(TAG, "Attempting to fetch rental history for user ID: " + userId);

        db.collection("rental_requests")
                .whereEqualTo("userId", userId)
                .orderBy("startDate", Query.Direction.DESCENDING) // Order by start date, newest first
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        requestList.clear();
                        int documentCount = task.getResult() != null ? task.getResult().size() : 0;
                        Log.d(TAG, "Firestore query successful. Found " + documentCount + " documents for user ID: " + userId);

                        if (documentCount > 0) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "Processing document ID: " + document.getId() + " => " + document.getData());
                                try {
                                    RentalRequest request = document.toObject(RentalRequest.class);
                                    request.setRequestId(document.getId()); // Set the document ID
                                    // Log the converted object details
                                    Log.d(TAG, "Converted RentalRequest: Model=" + request.getCarModel() +
                                            ", StartDate=" + request.getStartDate() +
                                            ", EndDate=" + request.getEndDate() +
                                            ", Status=" + request.getStatus() +
                                            ", UserID=" + request.getUserId());
                                    requestList.add(request);
                                } catch (Exception e) {
                                    // Log any error during object conversion
                                    Log.e(TAG, "Error converting document " + document.getId() + " to RentalRequest object.", e);
                                }
                            }
                            Log.d(TAG, "Finished processing documents. Total requests added to list: " + requestList.size());
                            adapter.notifyDataSetChanged();
                            requestsRecyclerView.setVisibility(View.VISIBLE);
                            tvNoRequests.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG, "No rental requests found for this user in Firestore.");
                            requestsRecyclerView.setVisibility(View.GONE);
                            tvNoRequests.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e(TAG, "Error getting rental requests: ", task.getException());
                        // Log specific Firestore error codes if available
                        if (task.getException() instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) task.getException();
                            Log.e(TAG, "Firestore Error Code: " + firestoreEx.getCode());
                        }
                        Toast.makeText(getContext(), "Error fetching rental history: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        requestsRecyclerView.setVisibility(View.GONE);
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText("Error loading history."); // Update text on error
                    }
                });
    }
}
