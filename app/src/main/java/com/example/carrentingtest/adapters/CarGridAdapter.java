package com.example.carrentingtest.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentingtest.R;
import com.example.carrentingtest.models.Car;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CarGridAdapter extends RecyclerView.Adapter<CarGridAdapter.CarViewHolder> {

    public interface OnCarClickListener {
        void onCarClicked(Car car);
    }

    private final List<Car> cars;
    private final OnCarClickListener listener;

    public CarGridAdapter(List<Car> cars, OnCarClickListener listener) {
        this.cars = cars;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = cars.get(position);
        holder.bind(car, listener);
    }

    @Override
    public int getItemCount() {
        return cars.size();
    }

    static class CarViewHolder extends RecyclerView.ViewHolder {

        private final ImageView carImage;
        private final TextView carModel;
        private final TextView carType;
        private final TextView carTransmission;
        private final TextView carSeats;
        private final TextView carPrice;
        private final TextView carAvailability;
        private final MaterialButton rentButton;
        private final MaterialCardView rootCard;

        CarViewHolder(@NonNull View itemView) {
            super(itemView);
            carImage = itemView.findViewById(R.id.carImage);
            carModel = itemView.findViewById(R.id.carModel);
            carType = itemView.findViewById(R.id.carType);
            carTransmission = itemView.findViewById(R.id.carTransmission);
            carSeats = itemView.findViewById(R.id.carSeats);
            carPrice = itemView.findViewById(R.id.carPrice);
            carAvailability = itemView.findViewById(R.id.carAvailability);
            rentButton = itemView.findViewById(R.id.rentButton);
            rootCard = (MaterialCardView) itemView;
        }

        void bind(Car car, OnCarClickListener listener) {
            carModel.setText(car.getModel());
            carType.setText(car.getType());

            String transmission = car.getTransmissionType();
            if (transmission == null || transmission.trim().isEmpty()) {
                transmission = itemView.getContext().getString(R.string.transmission_unknown);
            }
            carTransmission.setText(transmission);

            int seats = car.getSeats();
            String seatsText = seats > 0
                    ? itemView.getResources().getQuantityString(R.plurals.car_seats_count, seats, seats)
                    : itemView.getContext().getString(R.string.seats_unknown);
            carSeats.setText(seatsText);

            carPrice.setText(String.format(itemView.getContext().getString(R.string.price_per_day_format), car.getPricePerDay()));

            if (car.isAvailable()) {
                carAvailability.setText(itemView.getContext().getString(R.string.car_available));
                carAvailability.setVisibility(View.VISIBLE);
                rentButton.setEnabled(true);
                rentButton.setAlpha(1f);
            } else {
                carAvailability.setVisibility(View.GONE);
                rentButton.setEnabled(false);
                rentButton.setAlpha(0.6f);
            }

            Glide.with(itemView.getContext())
                    .load(car.getImageUrl())
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .centerCrop()
                    .into(carImage);

            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onCarClicked(car);
                }
            };

            rootCard.setOnClickListener(clickListener);
            rentButton.setOnClickListener(clickListener);
        }
    }
}
