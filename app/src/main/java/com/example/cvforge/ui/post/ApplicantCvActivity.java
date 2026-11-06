package com.example.cvforge.ui.post;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ActivityApplicantCvBinding;
import com.example.cvforge.ui.cv.CvTemplate;
import com.example.cvforge.util.AnimUtils;
import com.google.gson.Gson;

/** Shows the CV an applicant sent, drawn with the template the student chose. */
public class ApplicantCvActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_CV_JSON = "cv_json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityApplicantCvBinding binding = ActivityApplicantCvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(getIntent().getStringExtra(EXTRA_NAME));
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        Cv cv = new Gson().fromJson(getIntent().getStringExtra(EXTRA_CV_JSON), Cv.class);
        binding.preview.setTemplate(CvTemplate.fromId(cv.template));
        binding.preview.setCv(cv);
        AnimUtils.slideUpIn(binding.cardCv, 0);
    }
}
