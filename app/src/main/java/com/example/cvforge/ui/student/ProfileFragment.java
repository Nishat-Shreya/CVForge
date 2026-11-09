package com.example.cvforge.ui.student;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.EndorsementRepository;
import com.example.cvforge.data.MentorRepository;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.Endorsement;
import com.example.cvforge.data.model.MentorSlot;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.FragmentProfileBinding;
import com.example.cvforge.databinding.ItemBadgeBinding;
import com.example.cvforge.ui.auth.LogoutHelper;
import com.example.cvforge.ui.cv.CvEditorActivity;
import com.example.cvforge.ui.profile.BookSlotActivity;
import com.example.cvforge.ui.profile.EndorsementAdapter;
import com.example.cvforge.ui.profile.RequestEndorsementActivity;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.StreakTracker;

import java.util.Date;
import java.util.List;

/**
 * Profile tab (matches Docs/07-profile.png): header, name, stats, badges, verified
 * endorsements and the next mentor slot. Everything is read again when the tab comes back.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private final EndorsementAdapter adapter = new EndorsementAdapter();
    private final UserRepository userRepository = new UserRepository();
    private ObjectAnimator avatarFloat;

    private Cv latestCv;
    private int endorsementCount;
    private boolean statsAnimated;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.listEndorsements.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listEndorsements.setAdapter(adapter);

        binding.btnEdit.setOnClickListener(v -> editProfile());
        binding.btnRequestEndorsement.setOnClickListener(v -> open(RequestEndorsementActivity.class));
        binding.btnBookMentor.setOnClickListener(v -> open(BookSlotActivity.class));
        binding.btnBookSlot.setOnClickListener(v -> open(BookSlotActivity.class));
        binding.btnLogout.setOnClickListener(v -> LogoutHelper.logOut(requireActivity()));

        // A4: the sections slide up one after the other
        AnimUtils.slideUpIn(binding.cardStats, 0);
        AnimUtils.slideUpIn(binding.rowBadges, 1);
        AnimUtils.slideUpIn(binding.listEndorsements, 2);
        // the avatar floats gently, like A5
        avatarFloat = AnimUtils.floatUpDown(binding.textInitials, 0);

    }

    /** Reloads when the student comes back, so a new CV or endorsement shows up. */
    @Override
    public void onResume() {
        super.onResume();
        loadUser();
        loadCvAndStreak();
        loadEndorsements();
        loadNextSlot();
    }

    // ---------- loading ----------

    private void loadUser() {
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                if (binding == null) {
                    return;
                }
                binding.textName.setText(user.getName());
                binding.textInitials.setText(initials(user.getName()));
                showMeta(user);
            }

            @Override
            public void onError(String message) {
                showMessage(message);
            }
        });
    }

    private void loadCvAndStreak() {
        String uid = userRepository.getCurrentUid();
        binding.textStatStreak.setText(String.valueOf(StreakTracker.current(requireContext(), uid)));
        new CvRepository(requireContext()).loadAll(uid, cvs -> {
            if (binding == null) {
                return;
            }
            latestCv = cvs.isEmpty() ? null : cvs.get(0);
            showMeta(null);
            showBadges();
        });
    }

    private void loadEndorsements() {
        new EndorsementRepository().loadApproved(userRepository.getCurrentUid(),
                new EndorsementRepository.ListListener() {
                    @Override
                    public void onLoaded(List<Endorsement> endorsements) {
                        if (binding == null) {
                            return;
                        }
                        adapter.setItems(endorsements);
                        endorsementCount = endorsements.size();
                        binding.textNoEndorsements.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
                        showBadges();
                    }

                    @Override
                    public void onError(String message) {
                        if (binding != null) {
                            binding.textNoEndorsements.setText(R.string.error_loading);
                            binding.textNoEndorsements.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    /** The soonest open slot of an alumni, shown as the coral card. No slot: the card stays hidden. */
    private void loadNextSlot() {
        new MentorRepository().loadOpen(new MentorRepository.ListListener() {
            @Override
            public void onLoaded(List<MentorSlot> slots) {
                if (binding == null) {
                    return;
                }
                if (slots.isEmpty()) {
                    binding.cardSlot.setVisibility(View.GONE);
                    return;
                }
                MentorSlot slot = slots.get(0);
                binding.cardSlot.setVisibility(View.VISIBLE);
                binding.textSlotInitials.setText(initials(slot.mentorName));
                binding.textSlotTitle.setText(getString(R.string.slot_title, slot.mentorName));
                binding.textSlotTime.setText(DateFormat.format("EEE h:mm a", new Date(slot.startAt))
                        + " · " + getString(R.string.slot_minutes, MentorSlot.LENGTH_MINUTES));
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    binding.cardSlot.setVisibility(View.GONE);
                }
            }
        });
    }

    // ---------- showing ----------

    private User user;

    /** "CSE · KUET · Student": the first education entry of the CV and the role. */
    private void showMeta(@Nullable User loadedUser) {
        if (loadedUser != null) {
            user = loadedUser;
        }
        if (user == null) {
            return;
        }
        StringBuilder meta = new StringBuilder();
        if (latestCv != null && latestCv.education != null && !latestCv.education.isEmpty()) {
            Cv.Education education = latestCv.education.get(0);
            append(meta, education.degree);
            append(meta, education.school);
        }
        append(meta, getString(R.string.role_student));
        binding.textMeta.setText(meta.toString());
    }

    private void append(StringBuilder text, @Nullable String part) {
        if (part != null && !part.trim().isEmpty()) {
            text.append(text.length() == 0 ? "" : " · ").append(part.trim());
        }
    }

    /**
     * Four badges. A badge that is not earned yet is grey, so the student sees what to do next:
     * First CV (has a CV), Verified (an endorsement), the streak, GitHub (an imported project).
     */
    private void showBadges() {
        if (binding == null) {
            return;
        }
        int streak = StreakTracker.current(requireContext(), userRepository.getCurrentUid());
        boolean hasGithub = false;
        if (latestCv != null && latestCv.projects != null) {
            for (Cv.Project project : latestCv.projects) {
                hasGithub |= project.fromGithub;
            }
        }
        boolean[] earned = {latestCv != null, endorsementCount > 0, streak >= 3, hasGithub};
        String streakLabel = streak >= 3 ? getString(R.string.badge_days, streak) : getString(R.string.badge_streak);

        binding.rowBadges.removeAllViews();
        addBadge(R.drawable.ic_star, R.color.amber, getString(R.string.badge_first_cv), earned[0], true);
        addBadge(R.drawable.ic_check, R.color.teal, getString(R.string.badge_verified), earned[1], false);
        addBadge(R.drawable.ic_flame, R.color.coral, streakLabel, earned[2], true);
        addBadge(R.drawable.ic_branch, R.color.violet, getString(R.string.badge_github), earned[3], false);

        int count = 0;
        for (boolean value : earned) {
            count += value ? 1 : 0;
        }
        showStats(count);
    }

    private void addBadge(int icon, int colorRes, String label, boolean earned, boolean darkIcon) {
        Context context = requireContext();
        ItemBadgeBinding badge = ItemBadgeBinding.inflate(LayoutInflater.from(context), binding.rowBadges, false);
        badge.imageBadge.setImageResource(icon);
        badge.imageBadge.setBackgroundTintList(ContextCompat.getColorStateList(context,
                earned ? colorRes : R.color.border));
        // an earned badge has the strong color, a missing one is grey with a muted icon
        int iconColor = !earned ? R.color.muted : darkIcon ? R.color.ink : R.color.white;
        badge.imageBadge.setImageTintList(ContextCompat.getColorStateList(context, iconColor));
        badge.textBadge.setText(label);
        // the state is also said in words, not only by color
        badge.getRoot().setContentDescription(getString(earned ? R.string.badge_earned : R.string.badge_locked, label));
        binding.rowBadges.addView(badge.getRoot());
    }

    /** A1 style count-up, only the first time. */
    private void showStats(int badges) {
        if (!statsAnimated) {
            statsAnimated = true;
            AnimUtils.countUp(binding.textStatEndorsements, endorsementCount);
            AnimUtils.countUp(binding.textStatBadges, badges);
        } else {
            binding.textStatEndorsements.setText(String.valueOf(endorsementCount));
            binding.textStatBadges.setText(String.valueOf(badges));
        }
    }

    // ---------- actions ----------

    /** The edit button opens the CV editor, where the name, school and skills are kept. */
    private void editProfile() {
        Intent intent = new Intent(requireContext(), CvEditorActivity.class);
        if (latestCv != null) {
            intent.putExtra(CvEditorActivity.EXTRA_CV_ID, latestCv.id);
        }
        startActivity(intent);
        AnimUtils.fadeTransition(requireActivity());
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(requireContext(), screen));
        AnimUtils.fadeTransition(requireActivity());
    }

    private void showMessage(String message) {
        if (binding != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (avatarFloat != null) {
            avatarFloat.cancel();
            avatarFloat = null;
        }
        binding = null;
    }
}
