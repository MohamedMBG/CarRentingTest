package com.example.carrentingtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Adapter for displaying rental requests in a RecyclerView
public class ClientRentalRequestAdapter extends RecyclerView.Adapter<ClientRentalRequestAdapter.ViewHolder> {

    private final List<RentalRequest> requests; // List of rental requests to display
    private final Context context; // Application context for resources

    // Constructor: Initializes with context and data list
    public ClientRentalRequestAdapter(Context context, List<RentalRequest> requests) {
        this.context = context;
        this.requests = requests;
    }

    /**
     * Creates new ViewHolder instances when needed
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new ViewHolder that holds the inflated view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout and create ViewHolder
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_client_rental_request, parent, false));
    }

    /**
     * Binds data to the views for a specific position
     * @param h The ViewHolder to bind data to
     * @param p The position in the data list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int p) {
        RentalRequest r = requests.get(p); // Get request at position

        // Set car model text with null check
        h.tvCarModel.setText(r.getCarModel() != null ? r.getCarModel() : "Unknown");

        // Set formatted date range
        h.tvDates.setText(formatDates(r.getStartDate(), r.getEndDate()));

        // Set status text with null check
        h.tvStatus.setText("Status: " + (r.getStatus() != null ? r.getStatus() : "Unknown"));

        // Set status text color
        h.tvStatus.setTextColor(getStatusColor(r.getStatus()));

        // Show/hide additional requests based on availability
        boolean hasRequests = r.getAdditionalRequests() != null && !r.getAdditionalRequests().isEmpty();
        h.tvRequests.setVisibility(hasRequests ? View.VISIBLE : View.GONE);
        if (hasRequests) {
            h.tvRequests.setText("Requests: " + r.getAdditionalRequests());
        }
    }

    /**
     * Returns total number of items in data set
     * @return The total count of rental requests
     */
    @Override
    public int getItemCount() {
        return requests.size();
    }

    /**
     * ViewHolder class that holds references to all views in an item
     */
    /*
     * The ViewHolder is a static inner class that holds references to all views in a single list item.
     * Its purpose is to:
     * 1. Cache view references (via findViewById) when first created
     * 2. Allow RecyclerView to reuse these views when scrolling
     * 3. Improve performance by avoiding repeated findViewById calls
     *
     * This pattern is what makes RecyclerView more efficient than ListView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCarModel, tvDates, tvStatus, tvRequests;

        // Constructor: Finds and stores all views
        ViewHolder(View v) {
            super(v);
            tvCarModel = v.findViewById(R.id.tvClientCarModel);
            tvDates = v.findViewById(R.id.tvClientDates);
            tvStatus = v.findViewById(R.id.tvClientStatus);
            tvRequests = v.findViewById(R.id.tvClientAdditionalRequests);
        }
    }

    /**
     * Formats date range string from two Date objects
     * @param s Start date (nullable)
     * @param e End date (nullable)
     * @return Formatted date range string
     */
    private String formatDates(Date s, Date e) {
        SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        // Uses epoch (Jan 1, 1970) as fallback for null dates
        return f.format(s != null ? s : new Date(0)) + " to " + f.format(e != null ? e : new Date(0));
    }

    /**
     * Determines color based on request status
     * @param s Status string (nullable)
     * @return Color resource ID based on status
     */
    private int getStatusColor(String s) {
        if (s == null) return ContextCompat.getColor(context, R.color.colorWarning);
        switch (s.toLowerCase()) {
            case "approved": return ContextCompat.getColor(context, R.color.colorSuccess);
            case "rejected": return ContextCompat.getColor(context, R.color.colorError);
            default: return ContextCompat.getColor(context, R.color.colorWarning);
        }
    }
}