package com.example.cvforge.ui.student;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.cvforge.R;
import com.example.cvforge.databinding.ActivityStudentBinding;
import com.example.cvforge.receiver.NetworkReceiver;
import com.example.cvforge.service.CvForgeMessagingService;
import com.example.cvforge.ui.tracker.ApplicationDetailActivity;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.NotificationHelper;
import com.example.cvforge.util.NotificationPermission;
import com.example.cvforge.util.ReminderScheduler;

/** Student home: five tabs in a bottom navigation bar, one fragment per tab. */
public class StudentActivity extends AppCompatActivity {

    public static final int TAB_HOME = 0;
    public static final int TAB_DISCOVER = 1;
    public static final int TAB_CV = 2;
    public static final int TAB_TRACKER = 3;
    public static final int TAB_PROFILE = 4;

    private static final String KEY_TAB = "tab";

    private ActivityStudentBinding binding;
    private ObjectAnimator dotPulse;

    /** Shows or hides the offline banner when the connection changes. */
    private final NetworkReceiver networkReceiver = new NetworkReceiver(this::showOnline);

    private NotificationPermission notificationPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.createChannel(this);
        notificationPermission = new NotificationPermission(this);
        if (savedInstanceState == null) {
            notificationPermission.requestIfNeeded();
        }
        // keeps the reminder alarms up to date in the background (WorkManager)
        ReminderScheduler.enqueueRefresh(this);
        ReminderScheduler.enqueueDigest(this);
        binding = ActivityStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.addTab(R.string.tab_home, R.drawable.ic_nav_home);
        binding.bottomNav.addTab(R.string.tab_discover, R.drawable.ic_nav_discover);
        binding.bottomNav.addTab(R.string.tab_cv, R.drawable.ic_nav_cv);
        binding.bottomNav.addTab(R.string.tab_tracker, R.drawable.ic_nav_tracker);
        binding.bottomNav.addTab(R.string.tab_profile, R.drawable.ic_nav_profile);
        binding.bottomNav.setOnTabSelectedListener(this::showTab);

        // a tap on a notification can open an application: "application:ID"
        if (savedInstanceState == null) {
            openTarget(getIntent().getStringExtra(CvForgeMessagingService.EXTRA_TARGET));
        }

        // after a rotation the fragment manager brings the open tab back by itself
        int tab = savedInstanceState == null ? TAB_HOME : savedInstanceState.getInt(KEY_TAB, TAB_HOME);
        binding.bottomNav.select(tab, false);
        if (savedInstanceState == null) {
            showTab(tab);
        }
    }

    /** W10 lifecycle: the receiver listens only while the screen is visible. */
    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        // the receiver only hears changes, so check the state once now
        showOnline(NetworkReceiver.isOnline(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkReceiver);
        if (dotPulse != null) {
            dotPulse.cancel();
            dotPulse = null;
        }
    }

    /** The banner slides in when the phone goes offline. A7: its dot pulses. */
    private void showOnline(boolean online) {
        if (online) {
            binding.bannerOffline.setVisibility(View.GONE);
            if (dotPulse != null) {
                dotPulse.cancel();
                dotPulse = null;
            }
            return;
        }
        if (binding.bannerOffline.getVisibility() != View.VISIBLE) {
            binding.bannerOffline.setVisibility(View.VISIBLE);
            AnimUtils.slideDownIn(binding.bannerOffline);
        }
        if (dotPulse == null) {
            dotPulse = AnimUtils.pulse(binding.dotOffline);
        }
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, binding.bottomNav.getSelectedIndex());
    }

    /** Opens the screen that a notification asked for. Other targets are added with the chat. */
    private void openTarget(String target) {
        String prefix = "application:";
        if (target != null && target.startsWith(prefix)) {
            Intent intent = new Intent(this, ApplicationDetailActivity.class);
            intent.putExtra(ApplicationDetailActivity.EXTRA_APPLICATION_ID, target.substring(prefix.length()));
            startActivity(intent);
        }
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
