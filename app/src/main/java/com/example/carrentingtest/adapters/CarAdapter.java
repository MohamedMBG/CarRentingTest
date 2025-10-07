package com.example.carrentingtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.carrentingtest.R;
import com.example.carrentingtest.models.Car;

import java.util.List;

public class CarAdapter extends ArrayAdapter<Car> {

    private final LayoutInflater inflater;
    private boolean setClientOrAdmin = false;

    public CarAdapter(Context context, List<Car> cars) {
        super(context, 0, cars);
        inflater = LayoutInflater.from(context);
    }

    public void setClientOrAdmin(boolean value) {
        this.setClientOrAdmin = value;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView carImage;
        TextView carModel;
        TextView carType;
        TextView carTransmission;
        TextView carSeats;
        TextView carPrice;
        TextView carAvailability;
        Button btnRent;
        Button btnEdit;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Car car = getItem(position);
        ViewHolder holder;


        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_car, parent, false);
            holder = new ViewHolder();

            holder.btnRent = convertView.findViewById(R.id.rentButton);
            holder.btnEdit = convertView.findViewById(R.id.btnEdit);


            holder.carImage = convertView.findViewById(R.id.carImage);
            holder.carModel = convertView.findViewById(R.id.carModel);
            holder.carType = convertView.findViewById(R.id.carType);
            holder.carTransmission = convertView.findViewById(R.id.carTransmission);
            holder.carSeats = convertView.findViewById(R.id.carSeats);
            holder.carPrice = convertView.findViewById(R.id.carPrice);
            holder.carAvailability = convertView.findViewById(R.id.carAvailability);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (setClientOrAdmin) {
            holder.btnRent.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.VISIBLE);
        } else {
            holder.btnRent.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.GONE);
        }

        if (car != null) {
            // Set texts
            holder.carModel.setText(car.getModel());
            holder.carType.setText(car.getType());

            String transmission = car.getTransmissionType();
            if (transmission == null || transmission.trim().isEmpty()) {
                transmission = getContext().getString(R.string.transmission_unknown);
            }
            holder.carTransmission.setText(transmission);

            int seats = car.getSeats();
            String seatsText = seats > 0
                    ? getContext().getResources().getQuantityString(R.plurals.car_seats_count, seats, seats)
                    : getContext().getString(R.string.seats_unknown);
            holder.carSeats.setText(seatsText);

            // Format price
            holder.carPrice.setText(String.format(getContext().getString(R.string.price_per_day_format), car.getPricePerDay()));

            // Load image with Glide
            Glide.with(getContext())
                    .load(car.getImageUrl())
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .centerCrop()
                    .into(holder.carImage);

            // Handle availability badge
            if (car.isAvailable()) {
                holder.carAvailability.setText(getContext().getString(R.string.car_available));
                holder.carAvailability.setVisibility(View.VISIBLE);
                holder.carAvailability.setBackgroundResource(R.drawable.bg_badge_luxury);
                holder.carAvailability.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.homeHighlightTitle));
            } else {
                holder.carAvailability.setVisibility(View.GONE);
            }
        }

        return convertView;
    }
}
