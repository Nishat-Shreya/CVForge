package com.example.cvforge.ui.cv;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.local.AppDatabase;
import com.example.cvforge.data.local.CvEntity;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ActivityCvEditorBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.UUID;

/**
 * Form for the CV: personal info and education. Save stores the CV in Room on this phone,
 * on a background thread. The other sections, Firestore and autosave come in later checkpoints.
 */
public class CvEditorActivity extends AppCompatActivity {

    private ActivityCvEditorBinding binding;
    private final Cv cv = new Cv();
    private EducationAdapter educationAdapter;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCvEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        uid = new UserRepository().getCurrentUid();

        binding.toolbar.textTitle.setText(R.string.edit_cv);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveCv());

        educationAdapter = new EducationAdapter(cv.education);
        binding.sectionEducation.textSectionTitle.setText(R.string.section_education);
        binding.sectionEducation.btnAdd.setText(R.string.add_education);
        binding.sectionEducation.btnAdd.setOnClickListener(v -> educationAdapter.addItem());
        binding.sectionEducation.listItems.setLayoutManager(new LinearLayoutManager(this));
        binding.sectionEducation.listItems.setAdapter(educationAdapter);

        // A4: the sections slide up one after the other
        AnimUtils.slideUpIn(binding.cardPersonal, 0);
        AnimUtils.slideUpIn(binding.sectionEducation.sectionRoot, 1);
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
