package com.example.cvforge.ui.discover;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.JobRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.ActivityInternshipDetailBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.MatchScore;

import com.google.gson.Gson;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Details of one internship, with Save and Apply (features 4 and 5). */
public class InternshipDetailActivity extends AppCompatActivity {

    /** Explicit Intent extra: the id of the internship to show. */
    public static final String EXTRA_INTERNSHIP_ID = "internship_id";
    /** For jobs from other websites: the whole job as JSON, because it is not in Firestore. */
    public static final String EXTRA_INTERNSHIP_JSON = "internship_json";

    private ActivityInternshipDetailBinding binding;
    private final InternshipRepository internshipRepository = new InternshipRepository();
    private final ApplicationRepository applicationRepository = new ApplicationRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ApplyFlow applyFlow = new ApplyFlow(this);

    private Internship internship;
    private String uid;
    /** The student's application to this internship, or null if there is none yet. */
    private Application application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInternshipDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        uid = userRepository.getCurrentUid();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveInternship());
        binding.btnApply.setOnClickListener(v -> startApply());
        binding.barActions.setVisibility(View.GONE);

        String json = getIntent().getStringExtra(EXTRA_INTERNSHIP_JSON);
        if (json != null) {
            internship = new Gson().fromJson(json, Internship.class);
            show(internship);
            loadApplication();
            return;
        }

        String id = getIntent().getStringExtra(EXTRA_INTERNSHIP_ID);
        // jobs from other websites are in the Room cache, CVForge posts are in Firestore
        if (Internship.isExternalId(id)) {
            new JobRepository(this).loadCachedById(id, this::onInternshipLoaded);
        } else {
            internshipRepository.loadById(id == null ? "" : id, this::onInternshipLoaded);
        }
    }

    private void onInternshipLoaded(Internship loaded) {
        if (isDestroyed()) {
            return;
        }
        if (loaded == null) {
            showError(getString(R.string.not_found));
            return;
        }
        internship = loaded;
        show(loaded);
        loadApplication();
    }

    // ---------- showing the internship ----------

    private void show(Internship post) {
        binding.textLogo.setText(initials(post.posterName));
        binding.textTitle.setText(post.title);
        binding.textMeta.setText(getString(R.string.post_meta, post.posterName, post.location));
        binding.textDescription.setText(post.description);
        binding.chipVerified.setVisibility(View.VISIBLE);
        binding.textWarning.setVisibility(post.flagged ? View.VISIBLE : View.GONE);

        binding.groupInfo.removeAllViews();
        addChip(binding.groupInfo, InternshipAdapter.typeName(this, post.type), R.color.violet_tint, R.color.violet_text);
        if (post.stipend > 0) {
            addChip(binding.groupInfo, getString(R.string.stipend_format, post.stipend), R.color.teal_tint, R.color.teal_text);
        } else {
            addChip(binding.groupInfo, getString(R.string.unpaid), R.color.sky_tint, R.color.sky_text);
        }
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(post.deadline));
        addChip(binding.groupInfo, getString(R.string.deadline_chip, date), R.color.amber_tint, R.color.amber_text);

        binding.groupSkills.removeAllViews();
        for (String skill : post.skills) {
            addChip(binding.groupSkills, skill, R.color.violet_tint, R.color.violet_text);
        }

        // a link to the company site, for jobs from other websites
        if (post.url != null && !post.url.isEmpty()) {
            binding.btnOpenLink.setVisibility(View.VISIBLE);
            binding.btnOpenLink.setOnClickListener(v -> openLink(post.url));
        }

        AnimUtils.slideUpIn(binding.contentBlock, 0);
        loadMatch(post);
    }

    /** Feature 8: compares the skills of the student CV with the skills of the role. */
    private void loadMatch(Internship post) {
        if (post.skills == null || post.skills.isEmpty()) {
            return;
        }
        new CvRepository(this).loadAll(uid, cvs -> {
            if (isDestroyed()) {
                return;
            }
            List<String> studentSkills = cvs.isEmpty() ? new java.util.ArrayList<>() : cvs.get(0).skills;
            showMatch(MatchScore.compare(studentSkills, post.skills), post);
        });
    }

    private void showMatch(MatchScore.Result result, Internship post) {
        binding.cardMatch.setVisibility(View.VISIBLE);
        binding.ringMatch.animateTo(result.percent, 1300);

        // radar: one axis for each of the first five required skills
        List<Boolean> hasSkill = new java.util.ArrayList<>();
        for (String skill : post.skills) {
            hasSkill.add(result.matched.contains(skill));
        }
        binding.radar.setData(post.skills, hasSkill);
        int visible = binding.radar.hasEnoughAxes() ? View.VISIBLE : View.GONE;
        binding.radar.setVisibility(visible);
        binding.legend.setVisibility(visible);
        binding.radar.animateIn();

        // matched skills are teal, missing skills are coral and say "missing"
        binding.groupMatch.removeAllViews();
        for (String skill : result.matched) {
            addChip(binding.groupMatch, skill, R.color.teal_tint, R.color.teal_text);
        }
        for (String skill : result.missing) {
            addChip(binding.groupMatch, getString(R.string.missing_format, skill), R.color.coral_tint, R.color.coral_text);
        }

        if (result.missing.isEmpty()) {
            binding.textMatchTip.setVisibility(View.GONE);
        } else {
            binding.textMatchTip.setVisibility(View.VISIBLE);
            binding.textMatchTip.setText(getString(R.string.match_tip, result.missing.get(0),
                    MatchScore.percentWithOneMore(result)));
        }
    }

    private void addChip(ViewGroup group, String text, int backgroundRes, int textRes) {
        TextView chip = (TextView) getLayoutInflater().inflate(R.layout.include_status_chip, group, false);
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(this, backgroundRes));
        chip.setTextColor(ContextCompat.getColor(this, textRes));
        group.addView(chip);
    }

    /** Implicit Intent: opens the link in the browser. */
    private void openLink(String url) {
        applyFlow.openLink(url);
    }

    // ---------- Save and Apply ----------

    /** Finds out if the student already saved or applied, then shows the bottom bar. */
    private void loadApplication() {
        applicationRepository.load(uid, internship.id, loaded -> {
            if (isDestroyed()) {
                return;
            }
            application = loaded;
            showButtons();
            binding.barActions.setVisibility(View.VISIBLE);
            AnimUtils.slideUpIn(binding.barActions, 1);
        });
    }

    private void showButtons() {
        boolean hasApplied = application != null && !Application.STATUS_SAVED.equals(application.status);
        boolean hasSaved = application != null;

        binding.btnSave.setEnabled(!hasSaved);
        if (hasApplied) {
            binding.btnApply.setText(R.string.applied);
            binding.btnApply.setEnabled(false);
        } else if (!internship.isOpen() || internship.deadline < System.currentTimeMillis()) {
            binding.btnApply.setText(R.string.closed_for_applications);
            binding.btnApply.setEnabled(false);
        } else {
            binding.btnApply.setText(R.string.apply);
            binding.btnApply.setEnabled(true);
        }
    }

    private void saveInternship() {
        binding.btnSave.setEnabled(false);
        applyFlow.save(internship, new ApplyFlow.Callback() {
            @Override
            public void onSuccess() {
                loadApplication();
            }

            @Override
            public void onFailure(String message) {
                binding.btnSave.setEnabled(true);
                if (message != null) {
                    showError(message);
                }
            }
        });
    }

    private void startApply() {
        binding.btnApply.setEnabled(false);
        applyFlow.apply(internship, new ApplyFlow.Callback() {
            @Override
            public void onSuccess() {
                loadApplication();
            }

            @Override
            public void onFailure(String message) {
                binding.btnApply.setEnabled(true);
                if (message != null) {
                    showError(message);
                }
            }
        });
    }

    private void showError(String message) {
        binding.textError.setText(message);
        binding.textError.setVisibility(View.VISIBLE);
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (parts[0].substring(0, 1) + second).toUpperCase();
    }
}
