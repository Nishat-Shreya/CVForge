package com.example.cvforge.ui.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

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
        } else {
            binding.chipInterview.setVisibility(View.GONE);
        }

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
