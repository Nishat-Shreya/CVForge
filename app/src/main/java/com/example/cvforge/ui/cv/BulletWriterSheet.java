package com.example.cvforge.ui.cv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cvforge.R;
import com.example.cvforge.ai.AiRepository;
import com.example.cvforge.ai.Prompts;
import com.example.cvforge.databinding.SheetBulletWriterBinding;
import com.example.cvforge.util.AnimUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet "Bullet writer": the student describes what they did, Gemini suggests three
 * CV bullet points, and a tap on a suggestion adds it to the CV. Nothing is added by itself.
 */
public class BulletWriterSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onBulletChosen(String role, String bullet);
    }

    private SheetBulletWriterBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetBulletWriterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnWriteBullets.setOnClickListener(v -> write());
    }

    private void write() {
        String role = binding.editBulletRole.getText().toString().trim();
        String work = binding.editBulletWork.getText().toString().trim();
        if (role.isEmpty() || work.isEmpty()) {
            showStatus(getString(R.string.bullet_fill_both));
            return;
        }
        binding.btnWriteBullets.setEnabled(false);
        binding.listBullets.removeAllViews();
        showStatus(getString(R.string.ai_writing));

        new AiRepository(requireContext()).generate(Prompts.bulletPoints(role, work),
                new AiRepository.Callback() {
                    @Override
                    public void onResult(String text) {
                        if (binding != null) {
                            showBullets(role, text);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (binding != null) {
                            binding.btnWriteBullets.setEnabled(true);
                            showStatus(message);
                        }
                    }
                });
    }

    private void showStatus(String text) {
        binding.textBulletStatus.setText(text);
        binding.textBulletStatus.setVisibility(View.VISIBLE);
    }

    /** One violet chip per line of the answer. */
    private void showBullets(String role, String text) {
        binding.btnWriteBullets.setEnabled(true);
        showStatus(getString(R.string.bullet_tap_to_add));
        int index = 0;
        for (String line : text.split("\n")) {
            String bullet = line.replaceFirst("^[-*•\\d.\\s]+", "").trim();
            if (!bullet.isEmpty()) {
                TextView chip = makeChip(role, bullet);
                binding.listBullets.addView(chip);
                AnimUtils.slideUpIn(chip, index++);
            }
        }
    }

    private TextView makeChip(String role, String bullet) {
        TextView chip = new TextView(requireContext(), null, 0, R.style.ChipTint_Violet);
        chip.setText(bullet);
        chip.setTextSize(14);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = (int) (10 * getResources().getDisplayMetrics().density);
        chip.setPadding(chip.getPaddingLeft(), pad, chip.getPaddingRight(), pad);
        chip.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        chip.setLayoutParams(params);
        chip.setOnClickListener(v -> {
            if (getActivity() instanceof Listener) {
                ((Listener) getActivity()).onBulletChosen(role, bullet);
            }
            chip.setAlpha(0.4f);
            chip.setEnabled(false);
        });
        return chip;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
