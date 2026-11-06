package com.example.cvforge.ui.tracker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.databinding.ActivityApplicationDetailBinding;
import com.example.cvforge.ui.discover.InternshipDetailActivity;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.CalendarHelper;
import com.example.cvforge.util.FollowUpEmail;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;

/**
 * One application: the current status, private notes and the status timeline.
 * The screen listens to Firestore while it is visible, so when a company changes the status
 * the chip and the timeline change by themselves.
 */
public class ApplicationDetailActivity extends AppCompatActivity {

    /** Explicit Intent extra: the id of the application. */
    public static final String EXTRA_APPLICATION_ID = "application_id";

    private ActivityApplicationDetailBinding binding;
    private final ApplicationRepository repository = new ApplicationRepository();
    private final TimelineAdapter timelineAdapter = new TimelineAdapter();

    private String applicationId;
    private Application application;
    private ListenerRegistration registration;
    /** The notes are copied into the text field only once, so live updates never erase typing. */
    private boolean notesShown;
    private boolean notesLoading;
    /** Asks for the calendar permissions. The answer decides how the interview is added. */
    private final ActivityResultLauncher<String[]> calendarPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> addInterviewToCalendar());
    private String savedNotes = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApplicationDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applicationId = getIntent().getStringExtra(EXTRA_APPLICATION_ID);

        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.listTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.listTimeline.setAdapter(timelineAdapter);
        binding.btnSaveNotes.setOnClickListener(v -> saveNotes(true));
        binding.btnOpenInternship.setOnClickListener(v -> openInternship());
        binding.btnAddCalendar.setOnClickListener(v -> onAddToCalendarClick());
        binding.btnFollowUp.setOnClickListener(v -> openFollowUpEmail());

        AnimUtils.slideUpIn(binding.cardStatus, 0);
    }

    /** Live updates only while the screen is visible. */
    @Override
    protected void onStart() {
        super.onStart();
        registration = repository.listenOne(applicationId, loaded -> {
            if (loaded == null || isDestroyed()) {
                return;
            }
            application = loaded;
            show(loaded);
        });
    }

    /** W8 lifecycle: the notes are saved when the screen goes away. */
    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        saveNotes(false);
    }

    private void show(Application loaded) {
        binding.toolbar.textTitle.setText(loaded.internshipTitle);
        binding.textCompany.setText(loaded.companyName);

        binding.chipStatus.setText(StatusStyle.labelRes(loaded.status));
        binding.chipStatus.setBackgroundTintList(ContextCompat.getColorStateList(this, StatusStyle.tintRes(loaded.status)));
        binding.chipStatus.setTextColor(ContextCompat.getColor(this, StatusStyle.textRes(loaded.status)));

        if (loaded.interviewAt > System.currentTimeMillis()) {
            CharSequence when = DateFormat.format("EEE d MMM, h:mm a", new Date(loaded.interviewAt));
            binding.chipInterview.setText(getString(R.string.chip_interview_at, when));
            binding.chipInterview.setVisibility(View.VISIBLE);
            binding.btnAddCalendar.setVisibility(View.VISIBLE);
        } else {
            binding.chipInterview.setVisibility(View.GONE);
            binding.btnAddCalendar.setVisibility(View.GONE);
        }

        // feature 13: no reply for 7 days, so a follow-up email can be drafted
        binding.btnFollowUp.setVisibility(loaded.isGhosted(System.currentTimeMillis()) ? View.VISIBLE : View.GONE);

        if (!notesLoading) {
            loadNotesOnce();
        }
        timelineAdapter.setSteps(loaded.statusHistory);
    }

    /** The notes are in their own collection that only the student can read. They are loaded once. */
    private void loadNotesOnce() {
        notesLoading = true;
        repository.loadNotes(applicationId, notes -> {
            if (isDestroyed()) {
                return;
            }
            savedNotes = notes;
            binding.editNotes.setText(notes);
            notesShown = true;
        });
    }

    /** Saves the notes. Only the student can read them, a company never sees them. */
    private void saveNotes(boolean showMessage) {
        if (application == null || !notesShown) {
            return;
        }
        String notes = binding.editNotes.getText().toString();
        if (notes.equals(savedNotes)) {
            return;
        }
        savedNotes = notes;
        repository.saveNotes(applicationId, application.studentId, notes, new ResultListener() {
            @Override
            public void onSuccess() {
                if (showMessage) {
                    Toast.makeText(ApplicationDetailActivity.this, R.string.notes_saved, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ApplicationDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** W10 runtime permission: explain first, then ask. Without the permission the calendar app opens instead. */
    private void onAddToCalendarClick() {
        if (application == null) {
            return;
        }
        if (CalendarHelper.hasPermission(this)) {
            addInterviewToCalendar();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.calendar_permission_title)
                .setMessage(R.string.calendar_permission_message)
                .setPositiveButton(R.string.allow, (dialog, which) -> calendarPermissionLauncher.launch(CalendarHelper.permissions()))
                .setNegativeButton(R.string.not_now, (dialog, which) -> addInterviewToCalendar())
                .show();
    }

    /** With permission: the event is written directly. Otherwise the calendar app opens with the event filled in. */
    private void addInterviewToCalendar() {
        if (application == null) {
            return;
        }
        if (CalendarHelper.hasPermission(this)
                && CalendarHelper.insertEvent(this, application.companyName, application.internshipTitle, application.interviewAt)) {
            Toast.makeText(this, R.string.calendar_added, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(CalendarHelper.insertIntent(this, application.companyName, application.internshipTitle, application.interviewAt));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_calendar_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Implicit Intent (ACTION_SENDTO): the email app opens with the follow-up text filled in. */
    private void openFollowUpEmail() {
        if (application == null) {
            return;
        }
        try {
            startActivity(FollowUpEmail.intent(this, application.studentName, application.companyName,
                    application.internshipTitle));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Opens the internship details (explicit Intent with the internship id). */
    private void openInternship() {
        if (application == null) {
            return;
        }
        Intent intent = new Intent(this, InternshipDetailActivity.class);
        intent.putExtra(InternshipDetailActivity.EXTRA_INTERNSHIP_ID, application.internshipId);
        startActivity(intent);
        AnimUtils.fadeTransition(this);
    }
}
