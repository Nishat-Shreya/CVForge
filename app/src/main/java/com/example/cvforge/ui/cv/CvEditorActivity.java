package com.example.cvforge.ui.cv;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.local.AppDatabase;
import com.example.cvforge.data.local.CvEntity;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ActivityCvEditorBinding;
import com.example.cvforge.databinding.IncludeListSectionBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.ListText;

import java.util.UUID;

/**
 * Form for the CV: personal info, skills, education, projects, experience,
 * certificates and links. Save stores the CV in Room on this phone, on a background thread.
 * Firestore and autosave come in checkpoint 16.
 */
public class CvEditorActivity extends AppCompatActivity {

    private ActivityCvEditorBinding binding;
    private final Cv cv = new Cv();
    private String uid;

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
        uid = new UserRepository().getCurrentUid();

        binding.toolbar.textTitle.setText(R.string.edit_cv);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveCv());

        setUpLists();
        playEntranceAnimation();
    }

    /** Creates the five adapters for the Cv and connects the Add buttons. */
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

    /** Room does not allow database calls on the main thread, so the insert runs in the executor. */
    private void saveCv() {
        readFields();
        if (cv.id == null) {
            cv.id = UUID.randomUUID().toString();
            cv.ownerUid = uid;
        }
        cv.updatedAt = System.currentTimeMillis();

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText(R.string.saving);
        CvEntity entity = CvEntity.fromCv(cv);
        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase.getInstance(getApplicationContext()).cvDao().insert(entity);
            runOnUiThread(() -> {
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText(R.string.save);
                Toast.makeText(this, R.string.cv_saved, Toast.LENGTH_SHORT).show();
            });
        });
    }
}
