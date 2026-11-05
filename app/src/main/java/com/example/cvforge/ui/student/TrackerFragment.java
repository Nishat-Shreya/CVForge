package com.example.cvforge.ui.student;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.databinding.FragmentTrackerBinding;
import com.example.cvforge.ui.tracker.KanbanAdapter;
import com.example.cvforge.util.AnimUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.List;

/** Tracker tab: the Kanban board with Saved, Applied, Interview and Offer (feature 5). */
public class TrackerFragment extends Fragment {

    private static final long HOUR_MS = 60L * 60 * 1000;

    private FragmentTrackerBinding binding;
    private KanbanAdapter kanbanAdapter;
    private ListenerRegistration registration;
    private ObjectAnimator countdownPulse;
    private final ApplicationRepository repository = new ApplicationRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTrackerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // tapping a card opens its details in a later checkpoint
        kanbanAdapter = new KanbanAdapter(application -> { });
        binding.listColumns.setAdapter(kanbanAdapter);

        AnimUtils.slideUpIn(binding.rowTitle, 0);
    }

    /**
     * Live updates: the board listens to Firestore only while the screen is visible, and it
     * changes by itself when a company changes the status of an application.
     */
    @Override
    public void onStart() {
        super.onStart();
        registration = repository.listen(new UserRepository().getCurrentUid(), new ApplicationRepository.ListListener() {
            @Override
            public void onChanged(List<Application> applications) {
                if (binding != null) {
                    kanbanAdapter.setApplications(applications);
                    showNextInterview(applications);
                }
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        stopCountdownPulse();
    }

    // ---------- next interview banner ----------

    /** Shows the interview that is closest in the future, or hides the banner. */
    private void showNextInterview(List<Application> applications) {
        long now = System.currentTimeMillis();
        Application next = null;
        for (Application application : applications) {
            if (application.interviewAt > now && (next == null || application.interviewAt < next.interviewAt)) {
                next = application;
            }
        }
        if (next == null) {
            binding.bannerInterview.setVisibility(View.GONE);
            stopCountdownPulse();
            return;
        }

        binding.bannerInterview.setVisibility(View.VISIBLE);
        binding.textBannerTitle.setText(getString(R.string.banner_interview, next.companyName));
        binding.textBannerTime.setText(DateFormat.format("EEE d MMM, h:mm a", new Date(next.interviewAt)));
        binding.textCountdown.setText(countdownText(next.interviewAt - now));
        if (countdownPulse == null) {
            countdownPulse = AnimUtils.pulse(binding.dotCountdown);
        }
    }

    private String countdownText(long millisLeft) {
        long hours = millisLeft / HOUR_MS;
        if (hours >= 48) {
            return getString(R.string.in_days, hours / 24);
        }
        if (hours >= 1) {
            return getString(R.string.in_hours, hours);
        }
        return getString(R.string.in_minutes, Math.max(1, millisLeft / (60L * 1000)));
    }

    private void stopCountdownPulse() {
        if (countdownPulse != null) {
            countdownPulse.cancel();
            countdownPulse = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
