package com.example.cvforge.ui.student;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.databinding.FragmentTrackerBinding;
import com.example.cvforge.ui.tracker.ApplicationDetailActivity;
import com.example.cvforge.ui.tracker.KanbanAdapter;
import com.example.cvforge.ui.tracker.KanbanColumn;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.ExactAlarmPermission;
import com.example.cvforge.util.ReminderScheduler;
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

        kanbanAdapter = new KanbanAdapter(this::openApplication, this::moveCard);
        binding.listColumns.setOnDragListener(this::scrollBoardWhileDragging);
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
                    // the alarms follow the board, for example when a company plans an interview
                    ReminderScheduler.schedule(requireContext(), applications);
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

    /** Explicit Intent: a tap on a card opens its notes and timeline. */
    private void openApplication(Application application) {
        Intent intent = new Intent(requireContext(), ApplicationDetailActivity.class);
        intent.putExtra(ApplicationDetailActivity.EXTRA_APPLICATION_ID, application.id);
        startActivity(intent);
        AnimUtils.fadeTransition(requireActivity());
    }

    // ---------- drag and drop ----------

    /** A card was dropped in another column: the new status is saved, and Offer starts confetti. */
    private void moveCard(Application application, KanbanColumn target) {
        repository.changeStatus(application, target.status, new ResultListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
        // the board already shows the move, because Firestore tells the listener about local writes at once
        if (target == KanbanColumn.OFFER) {
            binding.confetti.burst();
        }
    }

    /** While a card is dragged near the left or right edge, the board scrolls sideways. */
    private boolean scrollBoardWhileDragging(View board, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
            return event.getLocalState() instanceof Application;
        }
        if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
            float edge = 64 * getResources().getDisplayMetrics().density;
            if (event.getX() < edge) {
                binding.listColumns.scrollBy(-40, 0);
            } else if (event.getX() > board.getWidth() - edge) {
                binding.listColumns.scrollBy(40, 0);
            }
        }
        // the drop itself is handled by the column under the finger
        return event.getAction() != DragEvent.ACTION_DROP;
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
        ExactAlarmPermission.requestOnce(requireActivity());
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
