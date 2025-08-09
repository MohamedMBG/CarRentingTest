package com.example.carrentingtest.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;

public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.Holder> {
    private final LayoutInflater inflater;
    private final int[] layouts = new int[]{
            R.layout.onboarding_card_1,
            R.layout.onboarding_card_2,
            R.layout.onboarding_card_3
    };

    public OnboardingPagerAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(layouts[viewType], parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {}

    @Override
    public int getItemCount() { return layouts.length; }

    @Override
    public int getItemViewType(int position) { return position; }

    static class Holder extends RecyclerView.ViewHolder {
        Holder(@NonNull View itemView) { super(itemView); }
    }
}


