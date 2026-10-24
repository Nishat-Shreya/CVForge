package com.example.cvforge.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.FragmentCvBuilderBinding;
import com.example.cvforge.ui.cv.CvEditorActivity;
import com.example.cvforge.ui.cv.CvSample;
import com.example.cvforge.ui.cv.CvTemplate;
import com.example.cvforge.util.AnimUtils;

/** CV tab: template pills and a live preview of the newest CV. */
public class CvBuilderFragment extends Fragment {

    private FragmentCvBuilderBinding binding;
    private CvRepository cvRepository;
    private final UserRepository userRepository = new UserRepository();
    private Cv cv = new Cv();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCvBuilderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cvRepository = new CvRepository(requireContext());

        binding.pillModern.setOnClickListener(v -> chooseTemplate(CvTemplate.MODERN));
        binding.pillClassic.setOnClickListener(v -> chooseTemplate(CvTemplate.CLASSIC));
        binding.pillMinimal.setOnClickListener(v -> chooseTemplate(CvTemplate.MINIMAL));
        binding.btnEditCv.setOnClickListener(v -> openEditor());

        showTemplate(CvTemplate.MODERN);

        // A4: the blocks slide up one after the other
        AnimUtils.slideUpIn(binding.textTitle, 0);
        AnimUtils.slideUpIn(binding.rowTemplates, 1);
        AnimUtils.slideUpIn(binding.cardPreview, 2);
    }

    /** Loads the newest CV every time the tab is shown, so edits from the editor appear. */
    @Override
    public void onResume() {
        super.onResume();
        cvRepository.loadLatest(userRepository.getCurrentUid(), loaded -> {
            if (binding != null) {
                show(loaded);
            }
        });
    }

    private void show(Cv loaded) {
        cv = loaded == null ? new Cv() : loaded;
        // until the student saves a CV, the preview shows an example
        binding.preview.setCv(loaded == null ? CvSample.create() : cv);
        showTemplate(CvTemplate.fromId(cv.template));
    }

    private void chooseTemplate(CvTemplate template) {
        cv.template = template.id;
        showTemplate(template);
        if (cv.id != null) {
            cv.updatedAt = System.currentTimeMillis();
            cvRepository.saveDraft(cv);
        }
    }

    /** The chosen pill gets the solid accent color, the others a soft tint. */
    private void showTemplate(CvTemplate selected) {
        binding.preview.setTemplate(selected);
        stylePill(binding.pillModern, CvTemplate.MODERN, selected);
        stylePill(binding.pillClassic, CvTemplate.CLASSIC, selected);
        stylePill(binding.pillMinimal, CvTemplate.MINIMAL, selected);
    }

    private void stylePill(TextView pill, CvTemplate template, CvTemplate selected) {
        boolean isSelected = template == selected;
        int background = isSelected ? template.accentColor : template.tintColor;
        int text = isSelected ? R.color.white : template.textColor;
        pill.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), background));
        pill.setTextColor(ContextCompat.getColor(requireContext(), text));
    }

    /** Explicit Intent: opens the CV editor. */
    private void openEditor() {
        startActivity(new Intent(requireContext(), CvEditorActivity.class));
        AnimUtils.fadeTransition(requireActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
