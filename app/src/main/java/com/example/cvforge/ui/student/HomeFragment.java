package com.example.cvforge.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.FragmentHomeBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.StrengthScore;

/** Temporary Home tab with only the strength ring. The real Home screen comes in checkpoint 19. */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AnimUtils.slideUpIn(binding.textTitle, 0);
    }

    /** A1: the ring fills up every time Home is shown. */
    @Override
    public void onResume() {
        super.onResume();
        String uid = new UserRepository().getCurrentUid();
        new CvRepository(requireContext()).loadLatest(uid, loaded -> {
            if (binding != null) {
                showCv(loaded == null ? new Cv() : loaded);
            }
        });
    }

    private void showCv(Cv cv) {
        StrengthScore.Result result = StrengthScore.of(cv);
        binding.ringStrength.animateTo(result.score, 1400);
        binding.textTip.setText(result.tipRes == 0 ? R.string.strength_complete : result.tipRes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
