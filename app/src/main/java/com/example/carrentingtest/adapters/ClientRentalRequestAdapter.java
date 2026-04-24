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
import com.example.carrentingtest.domain.RentalRequestStatus;
import com.example.carrentingtest.models.RentalRequest;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Adapter for displaying rental requests in a RecyclerView
public class ClientRentalRequestAdapter extends RecyclerView.Adapter<ClientRentalRequestAdapter.ViewHolder> {

    private final List<RentalRequest> requests; // List of rental requests to display
    private final Context context; // Application context for resources
    private final SimpleDateFormat completedFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    private OnReportClickListener reportClickListener;

    // Constructor: Initializes with context and data list
    public ClientRentalRequestAdapter(Context context, List<RentalRequest> requests) {
        this.context = context;
        this.requests = requests;
    }

    public interface OnReportClickListener {
        void onReport(RentalRequest request);
    }

    public void setOnReportClickListener(OnReportClickListener listener) {
        this.reportClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_client_rental_request, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int p) {
        RentalRequest r = requests.get(p); // Get request at position

        h.tvCarModel.setText(r.getCarModel() != null ? r.getCarModel() : context.getString(R.string.active_rental_unknown_car));
        h.tvDates.setText(formatDates(r.getStartDate(), r.getEndDate()));
        bindStatus(h.tvStatus, RentalRequestStatus.from(r.getStatus()));

        Date completedAt = r.getCompletedAt();
        boolean showCompletion = completedAt != null
                && RentalRequestStatus.from(r.getStatus()) == RentalRequestStatus.COMPLETED;
        h.tvCompletedAt.setVisibility(showCompletion ? View.VISIBLE : View.GONE);
        if (showCompletion) {
            h.tvCompletedAt.setText(context.getString(R.string.request_completion_label) + ": "
                    + completedFormat.format(completedAt));
        } else {
            h.tvCompletedAt.setText(null);
        }

        boolean hasRequests = r.getAdditionalRequests() != null && !r.getAdditionalRequests().isEmpty();
        h.tvRequests.setVisibility(hasRequests ? View.VISIBLE : View.GONE);
        if (hasRequests) {
            h.tvRequests.setText(context.getString(R.string.request_special_request_label) + ": "
                    + r.getAdditionalRequests());
        } else {
            h.tvRequests.setText(null);
        }

        RentalRequestStatus status = RentalRequestStatus.from(r.getStatus());
        boolean canReport = status == RentalRequestStatus.APPROVED;
        if (canReport && reportClickListener != null) {
            h.btnReportIssue.setVisibility(View.VISIBLE);
            h.btnReportIssue.setOnClickListener(v -> reportClickListener.onReport(r));
        } else {
            h.btnReportIssue.setVisibility(View.GONE);
            h.btnReportIssue.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCarModel;
        final TextView tvDates;
        final TextView tvStatus;
        final TextView tvRequests;
        final TextView tvCompletedAt;
        final MaterialButton btnReportIssue;

        ViewHolder(View v) {
            super(v);
            tvCarModel = v.findViewById(R.id.tvClientCarModel);
            tvDates = v.findViewById(R.id.tvClientDates);
            tvStatus = v.findViewById(R.id.tvClientStatus);
            tvRequests = v.findViewById(R.id.tvClientAdditionalRequests);
            tvCompletedAt = v.findViewById(R.id.tvClientCompletedAt);
            btnReportIssue = v.findViewById(R.id.btnReportIssue);
        }
    }

    private String formatDates(Date s, Date e) {
        SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        return f.format(s != null ? s : new Date(0)) + " to " + f.format(e != null ? e : new Date(0));
    }

    private void bindStatus(TextView statusView, RentalRequestStatus status) {
        int backgroundRes;
        int textColor;
        int textRes;

        switch (status) {
            case APPROVED:
                backgroundRes = R.drawable.bg_status_approved;
                textColor = R.color.colorSuccess;
                textRes = R.string.request_status_approved_short;
                break;
            case COMPLETED:
                backgroundRes = R.drawable.bg_status_approved;
                textColor = R.color.colorSuccess;
                textRes = R.string.request_status_completed_short;
                break;
            case REJECTED:
                backgroundRes = R.drawable.bg_status_rejected;
                textColor = R.color.colorError;
                textRes = R.string.request_status_rejected_short;
                break;
            case PENDING:
                backgroundRes = R.drawable.bg_status_pending;
                textColor = R.color.colorWarning;
                textRes = R.string.request_status_pending_short;
                break;
            default:
                backgroundRes = R.drawable.bg_badge_soft;
                textColor = R.color.colorPrimary;
                textRes = R.string.request_status_unknown_short;
                break;
        }

        statusView.setBackgroundResource(backgroundRes);
        statusView.setText(textRes);
        statusView.setTextColor(ContextCompat.getColor(context, textColor));
    }
}
