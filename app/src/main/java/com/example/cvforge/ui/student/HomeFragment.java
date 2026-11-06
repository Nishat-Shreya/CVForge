package com.example.cvforge.ui.student;

import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.FragmentHomeBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.FollowUpEmail;
import com.example.cvforge.util.StreakTracker;
import com.example.cvforge.util.StrengthScore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Home tab, matches Docs/02-home.png. */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FocusAdapter focusAdapter;
    private ObjectAnimator bellPulse;
    private ListenerRegistration registration;
    private StrengthScore.Result lastStrength;
    private List<Application> applications = new ArrayList<>();
    private final UserRepository userRepository = new UserRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        focusAdapter = new FocusAdapter();
        binding.listFocus.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listFocus.setAdapter(focusAdapter);

        binding.textGreeting.setText(greetingForNow());
        binding.btnImproveCv.setOnClickListener(v -> openTab(StudentActivity.TAB_CV));
        // chat, notifications and "See all" are built in later checkpoints
        binding.btnChat.setOnClickListener(v -> showComingSoon());
        binding.btnBell.setOnClickListener(v -> showComingSoon());
        binding.textSeeAll.setOnClickListener(v -> showComingSoon());

        // the application numbers come with the application tracker
        binding.textStatApplied.setText("0");
        binding.textStatShortlisted.setText("0");
        binding.textStatInterviews.setText("0");

        showStreak();
        loadUser();
        playEntranceAnimation();
    }

    /** A4: the blocks slide up one after the other. A7: the bell dot pulses. */
    private void playEntranceAnimation() {
        AnimUtils.slideUpIn(binding.rowGreeting, 0);
        AnimUtils.slideUpIn(binding.rowChips, 1);
        AnimUtils.slideUpIn(binding.cardHero, 2);
        AnimUtils.slideUpIn(binding.rowStats, 3);
        AnimUtils.slideUpIn(binding.rowFocusTitle, 4);
        bellPulse = AnimUtils.pulse(binding.dotBell);
    }

    /** A1: the ring fills up every time Home is shown. */
    @Override
    public void onResume() {
        super.onResume();
        String uid = userRepository.getCurrentUid();
        new CvRepository(requireContext()).loadLatest(uid, loaded -> {
            if (binding != null) {
                showCv(loaded == null ? new Cv() : loaded);
            }
        });
    }

    private void showCv(Cv cv) {
        StrengthScore.Result result = StrengthScore.of(cv);
        binding.ringStrength.animateTo(result.score, 1400);
        binding.textTip.setText(result.tipRes == 0 ? R.string.strength_complete : result.tipRes);
        showLevel(result.score);
        showFocus(result);
    }

    private void showLevel(int score) {
        int level;
        int nameRes;
        if (score >= 80) {
            level = 4;
            nameRes = R.string.level_4;
        } else if (score >= 55) {
            level = 3;
            nameRes = R.string.level_3;
        } else if (score >= 25) {
            level = 2;
            nameRes = R.string.level_2;
        } else {
            level = 1;
            nameRes = R.string.level_1;
        }
        binding.chipLevel.setText(getString(R.string.level_format, level, getString(nameRes)));
    }

    /**
     * Today's Focus: a follow-up for an application with no reply for 7 days, the next interview,
     * improve the CV (while it is not complete) and browse internships.
     */
    private void showFocus(StrengthScore.Result result) {
        lastStrength = result;
        rebuildFocus();
    }

    private void rebuildFocus() {
        if (lastStrength == null || binding == null) {
            return;
        }
        List<FocusItem> items = new ArrayList<>();
        long now = System.currentTimeMillis();

        // feature 13: no reply for 7 days, with a button for a follow-up email
        for (Application application : applications) {
            if (application.isGhosted(now)) {
                long days = (now - application.lastChangeAt) / (24L * 60 * 60 * 1000);
                items.add(new FocusItem(initials(application.companyName),
                        getString(R.string.focus_no_reply_title, days),
                        getString(R.string.post_meta, application.companyName, application.internshipTitle),
                        getString(R.string.draft_followup), R.color.coral_tint, R.color.coral_text,
                        () -> openFollowUp(application)));
            }
        }

        // the next interview
        Application nextInterview = null;
        for (Application application : applications) {
            if (application.interviewAt > now
                    && (nextInterview == null || application.interviewAt < nextInterview.interviewAt)) {
                nextInterview = application;
            }
        }
        if (nextInterview != null) {
            CharSequence when = DateFormat.format("EEE d MMM, h:mm a", new Date(nextInterview.interviewAt));
            items.add(new FocusItem(initials(nextInterview.companyName),
                    getString(R.string.focus_interview_title, when),
                    getString(R.string.post_meta, nextInterview.companyName, nextInterview.internshipTitle),
                    getString(R.string.chip_interview), R.color.violet_tint, R.color.violet_text,
                    () -> openTab(StudentActivity.TAB_TRACKER)));
        }

        if (lastStrength.tipRes != 0) {
            items.add(new FocusItem("CV", getString(lastStrength.tipRes),
                    getString(R.string.focus_cv_subtitle, lastStrength.score), getString(R.string.chip_improve),
                    R.color.violet_tint, R.color.violet_text, () -> openTab(StudentActivity.TAB_CV)));
        }
        items.add(new FocusItem("IN", getString(R.string.focus_discover_title),
                getString(R.string.focus_discover_subtitle), getString(R.string.chip_browse),
                R.color.teal_tint, R.color.teal_text, () -> openTab(StudentActivity.TAB_DISCOVER)));
        focusAdapter.setItems(items);
    }

    /** Home listens to the applications while it is visible, so the focus cards stay current. */
    @Override
    public void onStart() {
        super.onStart();
        registration = new ApplicationRepository().listen(userRepository.getCurrentUid(), new ApplicationRepository.ListListener() {
            @Override
            public void onChanged(List<Application> list) {
                applications = list;
                rebuildFocus();
            }

            @Override
            public void onError(String message) {
                // Home still works without the application cards
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
    }

    /** Implicit Intent (ACTION_SENDTO): the email app opens with the follow-up text filled in. */
    private void openFollowUp(Application application) {
        try {
            startActivity(FollowUpEmail.intent(requireContext(), application.studentName,
                    application.companyName, application.internshipTitle));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.no_email_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void showStreak() {
        int days = StreakTracker.recordVisit(requireContext(), userRepository.getCurrentUid());
        binding.chipStreak.setText(getString(R.string.streak_days, days));
    }

    private void loadUser() {
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                if (binding == null) {
                    return;
                }
                String name = user.getName() == null ? "" : user.getName();
                binding.textName.setText(firstName(name));
                binding.avatar.textInitials.setText(initials(name));
            }

            @Override
            public void onError(String message) {
                // the greeting stays empty, Home still works
            }
        });
    }

    private String greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return getString(R.string.greeting_morning);
        }
        return getString(hour < 18 ? R.string.greeting_afternoon : R.string.greeting_evening);
    }

    private String firstName(String name) {
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts[0].isEmpty()) {
            return "?";
        }
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }

    private void openTab(int index) {
        ((StudentActivity) requireActivity()).openTab(index);
    }

    private void showComingSoon() {
        Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bellPulse != null) {
            bellPulse.cancel();
            bellPulse = null;
        }
        binding = null;
    }
}
