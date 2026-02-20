package com.example.carrentingtest.admin;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.carrentingtest.R;
import com.example.carrentingtest.utils.FullscreenUiHelper;

/**
 * Activity host for admin business reports.
 */
public class AdminReportsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);
        FullscreenUiHelper.apply(this, R.id.reports_container);

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.reports_container, new AdminReportsFragment());
            ft.commit();
        }
    }
}
