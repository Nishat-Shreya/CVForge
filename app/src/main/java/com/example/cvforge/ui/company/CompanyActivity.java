package com.example.cvforge.ui.company;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.cvforge.R;
import com.example.cvforge.databinding.ActivityCompanyBinding;

/** Company home: Dashboard, Postings, Applicants and Profile tabs. */
public class CompanyActivity extends AppCompatActivity {

    private static final String KEY_TAB = "tab";

    private ActivityCompanyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompanyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.addTab(R.string.tab_dashboard, R.drawable.ic_nav_dashboard);
        binding.bottomNav.addTab(R.string.tab_postings, R.drawable.ic_nav_postings);
        binding.bottomNav.addTab(R.string.tab_applicants, R.drawable.ic_nav_applicants);
        binding.bottomNav.addTab(R.string.tab_profile, R.drawable.ic_nav_profile);
        binding.bottomNav.setOnTabSelectedListener(this::showTab);

        int tab = savedInstanceState == null ? 0 : savedInstanceState.getInt(KEY_TAB, 0);
        binding.bottomNav.select(tab, false);
        if (savedInstanceState == null) {
            showTab(tab);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, binding.bottomNav.getSelectedIndex());
    }

    /** A12: the new tab fades in over the old one. */
    private void showTab(int index) {
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragmentContainer, createFragment(index))
                .commit();
    }

    /** All four tabs are placeholders for now. Only the Profile tab has the Log out button. */
    private Fragment createFragment(int index) {
        switch (index) {
            case 1:
                return TitleFragment.newInstance(R.string.tab_postings, false);
            case 2:
                return TitleFragment.newInstance(R.string.tab_applicants, false);
            case 3:
                return TitleFragment.newInstance(R.string.tab_profile, true);
            default:
                return TitleFragment.newInstance(R.string.tab_dashboard, false);
        }
    }
}
