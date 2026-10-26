package com.example.cvforge.ui.student;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.cvforge.R;
import com.example.cvforge.databinding.ActivityStudentBinding;

/** Student home: five tabs in a bottom navigation bar, one fragment per tab. */
public class StudentActivity extends AppCompatActivity {

    public static final int TAB_HOME = 0;
    public static final int TAB_DISCOVER = 1;
    public static final int TAB_CV = 2;
    public static final int TAB_TRACKER = 3;
    public static final int TAB_PROFILE = 4;

    private static final String KEY_TAB = "tab";

    private ActivityStudentBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.addTab(R.string.tab_home, R.drawable.ic_nav_home);
        binding.bottomNav.addTab(R.string.tab_discover, R.drawable.ic_nav_discover);
        binding.bottomNav.addTab(R.string.tab_cv, R.drawable.ic_nav_cv);
        binding.bottomNav.addTab(R.string.tab_tracker, R.drawable.ic_nav_tracker);
        binding.bottomNav.addTab(R.string.tab_profile, R.drawable.ic_nav_profile);
        binding.bottomNav.setOnTabSelectedListener(this::showTab);

        // after a rotation the fragment manager brings the open tab back by itself
        int tab = savedInstanceState == null ? TAB_HOME : savedInstanceState.getInt(KEY_TAB, TAB_HOME);
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

    private Fragment createFragment(int index) {
        switch (index) {
            case TAB_DISCOVER:
                return new DiscoverFragment();
            case TAB_CV:
                return new CvBuilderFragment();
            case TAB_TRACKER:
                return new TrackerFragment();
            case TAB_PROFILE:
                return new ProfileFragment();
            default:
                return new HomeFragment();
        }
    }

    /** Lets a fragment switch tab, for example the Improve CV button on Home. */
    public void openTab(int index) {
        binding.bottomNav.select(index, true);
        showTab(index);
    }
}
