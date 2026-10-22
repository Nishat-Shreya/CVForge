package com.example.cvforge.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cvforge.databinding.FragmentCvBuilderBinding;
import com.example.cvforge.ui.cv.CvEditorActivity;
import com.example.cvforge.util.AnimUtils;

/** CV tab. For now it only has a button that opens the CV editor (explicit Intent). */
public class CvBuilderFragment extends Fragment {

    private FragmentCvBuilderBinding binding;

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
        AnimUtils.slideUpIn(binding.textTitle, 0);
        binding.btnEditCv.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CvEditorActivity.class));
            AnimUtils.fadeTransition(requireActivity());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
