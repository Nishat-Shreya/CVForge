package com.example.cvforge.ui.cv;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ActivityCvEditorBinding;
import com.example.cvforge.databinding.IncludeListSectionBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.ListText;

import java.util.UUID;

/**
 * Form for the CV: personal info, skills, education, projects, experience,
 * certificates and links. Save writes to Room and to Firestore, and the draft is kept on
 * the phone every time the screen goes to the background (W8 lifecycle, onPause).
 */
public class CvEditorActivity extends AppCompatActivity {

    private ActivityCvEditorBinding binding;
    private CvRepository cvRepository;
    private String uid;
    private Cv cv = new Cv();

    private EducationAdapter educationAdapter;
    private ProjectAdapter projectAdapter;
    private ExperienceAdapter experienceAdapter;
    private CertificateAdapter certificateAdapter;
    private LinkAdapter linkAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCvEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cvRepository = new CvRepository(this);
        uid = new UserRepository().getCurrentUid();

        binding.toolbar.textTitle.setText(R.string.edit_cv);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveCv());

        setUpLists();
        playEntranceAnimation();
        loadCv();
    }

    /** Creates the five adapters for the current Cv and connects the Add buttons. */
    private void setUpLists() {
        educationAdapter = new EducationAdapter(cv.education);
        projectAdapter = new ProjectAdapter(cv.projects);
        experienceAdapter = new ExperienceAdapter(cv.experience);
        certificateAdapter = new CertificateAdapter(cv.certificates);
        linkAdapter = new LinkAdapter(cv.links);

        setUpSection(binding.sectionEducation, R.string.section_education, R.string.add_education,
                educationAdapter, v -> educationAdapter.addItem());
        setUpSection(binding.sectionProjects, R.string.section_projects, R.string.add_project,
                projectAdapter, v -> projectAdapter.addItem());
        setUpSection(binding.sectionExperience, R.string.section_experience, R.string.add_experience,
                experienceAdapter, v -> experienceAdapter.addItem());
        setUpSection(binding.sectionCertificates, R.string.section_certificates, R.string.add_certificate,
                certificateAdapter, v -> certificateAdapter.addItem());
        setUpSection(binding.sectionLinks, R.string.section_links, R.string.add_link,
                linkAdapter, v -> linkAdapter.addItem());
    }

    private void setUpSection(IncludeListSectionBinding section, int titleRes, int addRes,
                              RecyclerView.Adapter<?> adapter, View.OnClickListener onAdd) {
        section.textSectionTitle.setText(titleRes);
        section.btnAdd.setText(addRes);
        section.btnAdd.setOnClickListener(onAdd);
        section.listItems.setLayoutManager(new LinearLayoutManager(this));
        section.listItems.setAdapter(adapter);
    }

    /** A4: the sections slide up one after the other. */
    private void playEntranceAnimation() {
        AnimUtils.slideUpIn(binding.cardPersonal, 0);
        AnimUtils.slideUpIn(binding.cardSkills, 1);
        AnimUtils.slideUpIn(binding.sectionEducation.sectionRoot, 2);
        AnimUtils.slideUpIn(binding.sectionProjects.sectionRoot, 3);
    }

    /** Opens the newest saved CV of this student, if there is one (Room first, then Firestore). */
    private void loadCv() {
        cvRepository.loadLatest(uid, loaded -> {
            if (loaded != null && !isDestroyed()) {
                showCv(loaded);
            }
        });
    }

    private void showCv(Cv loaded) {
        cv = loaded;
        binding.editTitle.setText(cv.title);
        binding.editFullName.setText(cv.fullName);
        binding.editHeadline.setText(cv.headline);
        binding.editEmail.setText(cv.email);
        binding.editPhone.setText(cv.phone);
        binding.editLocation.setText(cv.location);
        binding.editSummary.setText(cv.summary);
        binding.editSkills.setText(ListText.join(cv.skills, ", "));

        setUpLists();
    }

    private void readFields() {
        String title = binding.editTitle.getText().toString().trim();
        cv.title = title.isEmpty() ? getString(R.string.default_cv_title) : title;
        cv.fullName = binding.editFullName.getText().toString().trim();
        cv.headline = binding.editHeadline.getText().toString().trim();
        cv.email = binding.editEmail.getText().toString().trim();
        cv.phone = binding.editPhone.getText().toString().trim();
        cv.location = binding.editLocation.getText().toString().trim();
        cv.summary = binding.editSummary.getText().toString().trim();
        cv.skills = ListText.split(binding.editSkills.getText().toString(), ",");
    }

    private void saveCv() {
        prepareForSave();
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText(R.string.saving);
        cvRepository.save(cv, new ResultListener() {
            @Override
            public void onSuccess() {
                finishSaving(getString(R.string.cv_saved));
            }

            @Override
            public void onError(String message) {
                finishSaving(getString(R.string.cv_saved_offline));
            }
        });
    }

    private void finishSaving(String message) {
        binding.btnSave.setEnabled(true);
        binding.btnSave.setText(R.string.save);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /** Reads the form and gives a new CV its id and owner. */
    private void prepareForSave() {
        readFields();
        if (cv.id == null) {
            cv.id = UUID.randomUUID().toString();
            cv.ownerUid = uid;
        }
        cv.updatedAt = System.currentTimeMillis();
    }

    /**
     * W8 lifecycle: when the screen goes to the background (home button, another app,
     * back) the draft is kept on the phone. An empty new CV is not saved.
     */
    @Override
    protected void onPause() {
        super.onPause();
        readFields();
        boolean hasContent = cv.id != null || !cv.fullName.isEmpty() || !cv.headline.isEmpty();
        if (hasContent) {
            prepareForSave();
            cvRepository.saveDraft(cv);
        }
    }
}
