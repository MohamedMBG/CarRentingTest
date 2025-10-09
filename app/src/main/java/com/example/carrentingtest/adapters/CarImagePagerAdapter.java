package com.example.carrentingtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentingtest.R;

import java.util.ArrayList;
import java.util.List;

public class CarImagePagerAdapter extends RecyclerView.Adapter<CarImagePagerAdapter.ImageViewHolder> {

    private final LayoutInflater inflater;
    private final List<String> imageUrls;

    public CarImagePagerAdapter(@NonNull Context context, List<String> imageUrls) {
        this.inflater = LayoutInflater.from(context);
        this.imageUrls = new ArrayList<>();

        if (imageUrls != null) {
            for (String url : imageUrls) {
                if (url != null && !url.trim().isEmpty()) {
                    this.imageUrls.add(url.trim());
                }
            }
        }

        if (this.imageUrls.isEmpty()) {
            this.imageUrls.add(null);
        }
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_car_image_page, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrls.get(position);

        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(R.drawable.car_placeholder)
                .error(R.drawable.car_placeholder)
                .centerCrop()
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
