package com.example.carrentingtest.ui.verify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;
import com.google.firebase.analytics.FirebaseAnalytics;

public class VerificationIntroFragment extends Fragment {
    private FirebaseAnalytics analytics;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verification_intro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        analytics = FirebaseAnalytics.getInstance(requireContext());
        analytics.logEvent("verification_intro_view", null);
        view.findViewById(R.id.btnStart).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.verification_container, new CaptureSelfieFragment())
                    .addToBackStack(null)
                    .commit();
        });
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());
    }
}


