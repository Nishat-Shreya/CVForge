package com.example.cvforge.ui.company;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.cvforge.R;
import com.example.cvforge.databinding.ActivityCompanyBinding;
import com.example.cvforge.ui.post.ApplicantsFragment;
import com.example.cvforge.util.NotificationHelper;
import com.example.cvforge.util.NotificationPermission;

/** Company home: Dashboard, Postings, Applicants and Profile tabs. */
public class CompanyActivity extends AppCompatActivity {

    private static final String KEY_TAB = "tab";

    private ActivityCompanyBinding binding;

    private NotificationPermission notificationPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.createChannel(this);
        notificationPermission = new NotificationPermission(this);
        if (savedInstanceState == null) {
            notificationPermission.requestIfNeeded();
        }
        binding = ActivityCompanyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.addTab(R.string.tab_dashboard, R.drawable.ic_nav_dashboard);
        binding.bottomNav.addTab(R.string.tab_postings, R.drawable.ic_nav_postings);
        binding.bottomNav.addTab(R.string.tab_applicants, R.drawable.ic_nav_applicants);
        binding.bottomNav.addTab(R.string.tab_profile, R.drawable.ic_nav_profile);
        binding.bottomNav.setOnTabSelectedListener(this::showTab);
        // back on another tab goes to the Dashboard first, back on the Dashboard leaves the app
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.bottomNav.getSelectedIndex() != 0) {
                    binding.bottomNav.select(0, true);
                    showTab(0);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

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
                return TitleFragment.newInstance(R.string.tab_postings, false, true);
            case 2:
                return new ApplicantsFragment();
            case 3:
                return TitleFragment.newInstance(R.string.tab_profile, true, false);
            default:
                return TitleFragment.newInstance(R.string.tab_dashboard, false, false);
        }
    }
}
