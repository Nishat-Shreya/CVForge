package com.example.cvforge.ui.discover;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.ai.AiRepository;
import com.example.cvforge.ai.Prompts;
import com.example.cvforge.data.RoadmapRepository;
import com.example.cvforge.data.RoadmapRepository.Step;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.databinding.ActivityRoadmapBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.ListText;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill gap roadmap (feature 11): Gemini suggests one learning step for each skill the
 * student is missing for an internship. The steps can be ticked, and they are saved on the phone.
 */
public class RoadmapActivity extends AppCompatActivity {

    public static final String EXTRA_INTERNSHIP_ID = "internship_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MISSING = "missing_skills";

    private ActivityRoadmapBinding binding;
    private RoadmapRepository repository;
    private StepAdapter adapter;
    private String uid;
    private String internshipId;
    private String title;
    private String missingSkills;
    private List<String> missingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoadmapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new RoadmapRepository(this);
        uid = new UserRepository().getCurrentUid();
        internshipId = getIntent().getStringExtra(EXTRA_INTERNSHIP_ID);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        List<String> missing = getIntent().getStringArrayListExtra(EXTRA_MISSING);
        missingList = missing == null ? new ArrayList<>() : missing;
        missingSkills = ListText.join(missingList, ", ");

        binding.toolbar.textTitle.setText(R.string.roadmap_title);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.textGoal.setText(getString(R.string.roadmap_goal, title, missingSkills));
        binding.btnRetry.setOnClickListener(v -> generate());

        adapter = new StepAdapter(this::onStepChanged);
        binding.listSteps.setLayoutManager(new LinearLayoutManager(this));
        binding.listSteps.setAdapter(adapter);

        List<Step> saved = repository.load(uid, internshipId);
        if (saved.isEmpty()) {
            generate();
        } else {
            showSteps(saved);
            if (saved.get(0).builtIn) {
                // the saved plan is the built-in one, so the AI can be asked again
                binding.btnRetry.setVisibility(View.VISIBLE);
            }
        }
    }

    private void generate() {
        binding.btnRetry.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.loading.setVisibility(View.VISIBLE);

        new AiRepository(this).generate(Prompts.roadmap(title, missingSkills),
                new AiRepository.Callback() {
                    @Override
                    public void onResult(String text) {
                        if (isDestroyed()) {
                            return;
                        }
                        List<Step> steps = RoadmapRepository.parse(text);
                        if (steps.isEmpty()) {
                            useBuiltInPlan(getString(R.string.ai_error_service));
                        } else {
                            binding.textNote.setVisibility(View.GONE);
                            repository.save(uid, internshipId, steps);
                            showSteps(steps);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!isDestroyed()) {
                            useBuiltInPlan(message);
                        }
                    }
                });
    }

    /**
     * The AI did not answer: the screen shows a simple plan with one step for each missing skill,
     * so it is never empty. If a plan is already shown (the student tapped Try again), it stays.
     */
    private void useBuiltInPlan(String reason) {
        binding.loading.setVisibility(View.GONE);
        if (adapter.getItemCount() == 0) {
            List<Step> steps = RoadmapRepository.builtIn(this, missingList);
            if (steps.isEmpty()) {
                showError();
                return;
            }
            repository.save(uid, internshipId, steps);
            showSteps(steps);
        }
        binding.textNote.setText(getString(R.string.roadmap_builtin_note, reason));
        binding.textNote.setVisibility(View.VISIBLE);
        binding.btnRetry.setVisibility(View.VISIBLE);
    }

    private void showSteps(List<Step> steps) {
        binding.loading.setVisibility(View.GONE);
        adapter.setSteps(steps);
        updateProgress(true);
        AnimUtils.slideUpIn(binding.listSteps, 0);
    }

    /** Only when there are no missing skills at all, so not even the built-in plan has a step. */
    private void showError() {
        binding.loading.setVisibility(View.GONE);
        binding.textError.setText(R.string.roadmap_failed);
        binding.textError.setVisibility(View.VISIBLE);
        binding.btnRetry.setVisibility(View.VISIBLE);
    }

    private void onStepChanged() {
        repository.save(uid, internshipId, adapter.getSteps());
        updateProgress(false);
    }

    /** A3: the bar grows to the share of ticked steps. */
    private void updateProgress(boolean animate) {
        List<Step> steps = adapter.getSteps();
        int done = 0;
        for (Step step : steps) {
            if (step.done) {
                done++;
            }
        }
        int percent = steps.isEmpty() ? 0 : Math.round(100f * done / steps.size());
        binding.textProgress.setText(getString(R.string.roadmap_progress, done, steps.size()));
        if (animate) {
            AnimUtils.growBar(binding.barProgress, percent);
        } else {
            binding.barProgress.setProgress(percent * 10);
        }
    }
}
