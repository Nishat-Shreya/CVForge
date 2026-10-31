package com.example.cvforge.ui.discover;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityInternshipDetailBinding;
import com.example.cvforge.util.AnimUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Details of one internship, with Save and Apply (features 4 and 5). */
public class InternshipDetailActivity extends AppCompatActivity {

    /** Explicit Intent extra: the id of the internship to show. */
    public static final String EXTRA_INTERNSHIP_ID = "internship_id";

    private ActivityInternshipDetailBinding binding;
    private final InternshipRepository internshipRepository = new InternshipRepository();
    private final ApplicationRepository applicationRepository = new ApplicationRepository();
    private final UserRepository userRepository = new UserRepository();

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

        String id = getIntent().getStringExtra(EXTRA_INTERNSHIP_ID);
        internshipRepository.loadById(id == null ? "" : id, this::onInternshipLoaded);
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

        AnimUtils.slideUpIn(binding.contentBlock, 0);
    }

    private void addChip(ViewGroup group, String text, int backgroundRes, int textRes) {
        TextView chip = (TextView) getLayoutInflater().inflate(R.layout.include_status_chip, group, false);
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(this, backgroundRes));
        chip.setTextColor(ContextCompat.getColor(this, textRes));
        group.addView(chip);
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
        withStudentName(name -> applicationRepository.save(uid, name, internship, new ResultListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(InternshipDetailActivity.this, R.string.save_done, Toast.LENGTH_SHORT).show();
                loadApplication();
            }

            @Override
            public void onError(String message) {
                binding.btnSave.setEnabled(true);
                showError(message);
            }
        }));
    }

    /** Apply needs a CV. With more than one version the student picks which one. */
    private void startApply() {
        binding.btnApply.setEnabled(false);
        new CvRepository(this).loadAll(uid, cvs -> {
            if (isDestroyed()) {
                return;
            }
            if (cvs.isEmpty()) {
                binding.btnApply.setEnabled(true);
                Toast.makeText(this, R.string.need_cv_first, Toast.LENGTH_LONG).show();
            } else if (cvs.size() == 1) {
                sendApplication(cvs.get(0));
            } else {
                chooseCv(cvs);
            }
        });
    }

    private void chooseCv(List<Cv> cvs) {
        String[] titles = new String[cvs.size()];
        for (int i = 0; i < cvs.size(); i++) {
            titles[i] = cvs.get(i).title;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_cv_title)
                .setItems(titles, (dialog, which) -> sendApplication(cvs.get(which)))
                .setOnCancelListener(dialog -> binding.btnApply.setEnabled(true))
                .show();
    }

    private void sendApplication(Cv cv) {
        withStudentName(name -> applicationRepository.apply(uid, name, internship, cv, new ResultListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(InternshipDetailActivity.this, R.string.apply_done, Toast.LENGTH_SHORT).show();
                loadApplication();
            }

            @Override
            public void onError(String message) {
                binding.btnApply.setEnabled(true);
                showError(message);
            }
        }));
    }

    private interface NameCallback {
        void onName(String name);
    }

    /** The application stores the student's name, so the company can see who applied. */
    private void withStudentName(NameCallback next) {
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                next.onName(user.getName());
            }

            @Override
            public void onError(String message) {
                showError(message);
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
