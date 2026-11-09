package com.example.cvforge.ui.post;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.ChatRepository;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.FragmentApplicantsBinding;
import com.example.cvforge.ui.chat.ChatActivity;
import com.example.cvforge.ui.chat.ChatsActivity;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.CalendarHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Feature 6: the poster sees the applicants of one internship with their CV and match,
 * and can shortlist, reject, plan an interview or send an offer.
 * The list listens to Firestore while it is visible, so new applicants appear by themselves.
 */
public class ApplicantsFragment extends Fragment implements ApplicantAdapter.Listener {

    private static final int TAB_NEW = 0;
    private static final int TAB_SHORTLISTED = 1;
    private static final int TAB_INTERVIEW = 2;

    private FragmentApplicantsBinding binding;
    private ApplicantAdapter adapter;
    private final ApplicationRepository applicationRepository = new ApplicationRepository();
    private final InternshipRepository internshipRepository = new InternshipRepository();
    private final String uid = new UserRepository().getCurrentUid();
    private ListenerRegistration registration;

    private List<Internship> posts = new ArrayList<>();
    private List<Application> applications = new ArrayList<>();
    private String selectedPostId;
    private int selectedTab = TAB_NEW;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentApplicantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ApplicantAdapter(this);
        binding.listApplicants.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listApplicants.setAdapter(adapter);

        binding.btnJob.setOnClickListener(v -> chooseInternship());
        binding.tabNew.setOnClickListener(v -> chooseTab(TAB_NEW));
        binding.tabShortlisted.setOnClickListener(v -> chooseTab(TAB_SHORTLISTED));
        binding.tabInterview.setOnClickListener(v -> chooseTab(TAB_INTERVIEW));
        binding.btnChat.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ChatsActivity.class));
            AnimUtils.fadeTransition(requireActivity());
        });

        AnimUtils.slideUpIn(binding.rowTitle, 0);
        AnimUtils.slideUpIn(binding.btnJob, 1);
        AnimUtils.slideUpIn(binding.rowTabs, 2);
    }

    // ---------- loading ----------

    @Override
    public void onStart() {
        super.onStart();
        binding.progress.setVisibility(View.VISIBLE);
        internshipRepository.loadByPoster(uid, new InternshipRepository.InternshipListListener() {
            @Override
            public void onLoaded(List<Internship> internships) {
                if (binding == null) {
                    return;
                }
                posts = internships;
                if (selectedPostId == null && !posts.isEmpty()) {
                    selectedPostId = posts.get(0).id;
                }
                startListening();
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    binding.progress.setVisibility(View.GONE);
                    showEmptyState(getString(R.string.error_loading), message);
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
    }

    private void startListening() {
        if (registration != null) {
            registration.remove();
        }
        registration = applicationRepository.listenByPoster(uid, new ApplicationRepository.ListListener() {
            @Override
            public void onChanged(List<Application> list) {
                if (binding == null) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                applications = list;
                showApplicants();
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    binding.progress.setVisibility(View.GONE);
                    showEmptyState(getString(R.string.error_loading), message);
                }
            }
        });
    }

    // ---------- showing ----------

    private void showApplicants() {
        if (posts.isEmpty()) {
            binding.btnJob.setText(R.string.no_posts_title);
            adapter.setApplicants(new ArrayList<>(), null);
            styleTabs(0, 0, 0);
            showEmptyState(getString(R.string.no_posts_title), getString(R.string.no_posts_message));
            return;
        }

        Internship post = selectedPost();
        List<Application> forPost = new ArrayList<>();
        int countNew = 0;
        int countShortlisted = 0;
        int countInterview = 0;
        for (Application application : applications) {
            if (!application.internshipId.equals(selectedPostId)) {
                continue;
            }
            forPost.add(application);
            if (Application.STATUS_APPLIED.equals(application.status)) {
                countNew++;
            } else if (Application.STATUS_SHORTLISTED.equals(application.status)) {
                countShortlisted++;
            } else if (Application.STATUS_INTERVIEW.equals(application.status)) {
                countInterview++;
            }
        }

        binding.btnJob.setText(getString(R.string.job_selector, post.title, forPost.size()));
        styleTabs(countNew, countShortlisted, countInterview);

        List<Application> shown = new ArrayList<>();
        for (Application application : forPost) {
            if (belongsToTab(application)) {
                shown.add(application);
            }
        }
        adapter.setApplicants(shown, post.skills);

        if (shown.isEmpty()) {
            showEmptyState(getString(R.string.no_applicants_title), getString(R.string.no_applicants_message));
        } else {
            binding.empty.emptyState.setVisibility(View.GONE);
        }
    }

    private boolean belongsToTab(Application application) {
        switch (selectedTab) {
            case TAB_SHORTLISTED:
                return Application.STATUS_SHORTLISTED.equals(application.status);
            case TAB_INTERVIEW:
                return Application.STATUS_INTERVIEW.equals(application.status);
            default:
                return Application.STATUS_APPLIED.equals(application.status);
        }
    }

    private Internship selectedPost() {
        for (Internship post : posts) {
            if (post.id.equals(selectedPostId)) {
                return post;
            }
        }
        return posts.get(0);
    }

    private void showEmptyState(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(View.VISIBLE);
    }

    /** The chosen tab has its strong color, the others a soft tint. New is violet, the others teal and amber. */
    private void styleTabs(int countNew, int countShortlisted, int countInterview) {
        styleTab(binding.tabNew, getString(R.string.tab_new, countNew), selectedTab == TAB_NEW,
                R.color.violet, R.color.white, R.color.violet_tint, R.color.violet_text);
        styleTab(binding.tabShortlisted, getString(R.string.tab_shortlisted, countShortlisted), selectedTab == TAB_SHORTLISTED,
                R.color.teal, R.color.ink, R.color.teal_tint, R.color.teal_text);
        styleTab(binding.tabInterview, getString(R.string.tab_interview, countInterview), selectedTab == TAB_INTERVIEW,
                R.color.amber, R.color.ink, R.color.amber_tint, R.color.amber_text);
    }

    private void styleTab(TextView tab, String text, boolean selected, int strong, int strongText, int tint, int tintText) {
        tab.setText(text);
        tab.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), selected ? strong : tint));
        tab.setTextColor(ContextCompat.getColor(requireContext(), selected ? strongText : tintText));
    }

    private void chooseTab(int tab) {
        selectedTab = tab;
        showApplicants();
    }

    private void chooseInternship() {
        if (posts.isEmpty()) {
            return;
        }
        String[] titles = new String[posts.size()];
        for (int i = 0; i < posts.size(); i++) {
            titles[i] = posts.get(i).title;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_internship)
                .setItems(titles, (dialog, which) -> {
                    selectedPostId = posts.get(which).id;
                    showApplicants();
                })
                .show();
    }

    // ---------- buttons on a card ----------

    /** Tap on a card: the CV the applicant sent. */
    @Override
    public void onOpenCv(Application application) {
        if (application.cv == null) {
            return;
        }
        Intent intent = new Intent(requireContext(), ApplicantCvActivity.class);
        intent.putExtra(ApplicantCvActivity.EXTRA_NAME, application.studentName);
        intent.putExtra(ApplicantCvActivity.EXTRA_CV_JSON, new Gson().toJson(application.cv));
        startActivity(intent);
        AnimUtils.fadeTransition(requireActivity());
    }

    /** Shortlist, plan an interview, or send an offer, depending on where the applicant is. */
    @Override
    public void onPrimaryAction(Application application) {
        switch (application.status) {
            case Application.STATUS_SHORTLISTED:
                pickInterviewTime(application);
                break;
            case Application.STATUS_INTERVIEW:
                changeStatus(application, Application.STATUS_OFFER);
                break;
            default:
                changeStatus(application, Application.STATUS_SHORTLISTED);
                break;
        }
    }

    @Override
    public void onReject(Application application) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.reject_title)
                .setMessage(getString(R.string.reject_message, application.studentName))
                .setPositiveButton(R.string.reject, (dialog, which) -> changeStatus(application, Application.STATUS_REJECTED))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Application Chat: the poster talks to the student about this application. */
    @Override
    public void onChat(Application application) {
        startActivity(ChatActivity.intent(requireContext(), ChatRepository.applicationChatId(application.id),
                application.studentId, application.studentName, User.ROLE_STUDENT, applicationContext(application)));
        AnimUtils.fadeTransition(requireActivity());
    }

    /** "Android Intern · Nexa Labs", shown in the banner of the chat. */
    private String applicationContext(Application application) {
        return getString(R.string.chat_context_application, application.internshipTitle, application.companyName);
    }

    private void changeStatus(Application application, String newStatus) {
        // a Cloud Function sends the student a push notification (checkpoint 44)
        applicationRepository.changeStatus(application, newStatus, new ResultListener() {
            @Override
            public void onSuccess() {
                // the list changes by itself, because it listens to Firestore
            }

            @Override
            public void onError(String message) {
                showMessage(message);
            }
        });
    }

    /** Date first, then time. */
    private void pickInterviewTime(Application application) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (picker, year, month, day) ->
                new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day, hour, minute, 0);
                    scheduleInterview(application, chosen.getTimeInMillis());
                }, 11, 0, false).show(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleInterview(Application application, long time) {
        applicationRepository.scheduleInterview(application, time, new ResultListener() {
            @Override
            public void onSuccess() {
                showMessage(getString(R.string.interview_planned));
                announceInterview(application, time);
                offerCalendar(application, time);
            }

            @Override
            public void onError(String message) {
                showMessage(message);
            }
        });
    }

    /** The "Interview set" notice appears in the chat of this application, for both people. */
    private void announceInterview(Application application, long time) {
        new UserRepository().loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                String when = DateFormat.format("EEE d MMM, h:mm a", new Date(time)).toString();
                new ChatRepository().announce(ChatRepository.applicationChatId(application.id),
                        new ChatRepository.Person(uid, user.getName(), user.getRole()),
                        new ChatRepository.Person(application.studentId, application.studentName, User.ROLE_STUDENT),
                        applicationContext(application),
                        getString(R.string.interview_set_message, when));
            }

            @Override
            public void onError(String message) {
                // the interview is planned anyway, only the chat notice is missing
            }
        });
    }

    /** The company can also put the interview in its own calendar (implicit Intent, no permission needed). */
    private void offerCalendar(Application application, long time) {
        if (binding == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.calendar_offer_title)
                .setMessage(getString(R.string.calendar_offer_message, application.studentName))
                .setPositiveButton(R.string.yes_add, (dialog, which) -> {
                    try {
                        startActivity(CalendarHelper.insertIntent(requireContext(), application.studentName,
                                application.internshipTitle, time));
                    } catch (ActivityNotFoundException e) {
                        showMessage(getString(R.string.no_calendar_app));
                    }
                })
                .setNegativeButton(R.string.not_now, null)
                .show();
    }

    private void showMessage(String message) {
        if (binding != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
