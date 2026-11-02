package com.example.cvforge.ui.discover;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cvforge.databinding.SheetFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;

/**
 * Bottom sheet where the student picks filters: skill, location, work type, deadline and paid.
 * The screen that opens it implements {@link Listener} and gets the new filter on Apply.
 */
public class FilterSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onFilterApplied(InternshipFilter filter);
    }

    private static final String ARG_FILTER = "filter";

    private SheetFilterBinding binding;

    public static FilterSheet newInstance(InternshipFilter current) {
        FilterSheet sheet = new FilterSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FILTER, current);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InternshipFilter current = (InternshipFilter) requireArguments().getSerializable(ARG_FILTER);
        showFilter(current == null ? new InternshipFilter() : current);

        binding.btnReset.setOnClickListener(v -> showFilter(new InternshipFilter()));
        binding.btnApply.setOnClickListener(v -> apply());
    }

    /** Puts the values of a filter into the form. */
    private void showFilter(InternshipFilter filter) {
        binding.editSkill.setText(filter.skill);
        binding.editLocation.setText(filter.location);
        selectChip(binding.groupType, filter.type);
        selectChip(binding.groupDeadline, String.valueOf(filter.deadlineDays));
        binding.switchPaid.setChecked(filter.paidOnly);
    }

    private void selectChip(ChipGroup group, String tag) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View chip = group.getChildAt(i);
            if (tag.equals(chip.getTag())) {
                group.check(chip.getId());
                return;
            }
        }
    }

    private String selectedTag(ChipGroup group) {
        View chip = group.findViewById(group.getCheckedChipId());
        return chip == null ? "" : (String) chip.getTag();
    }

    private void apply() {
        InternshipFilter filter = new InternshipFilter();
        // the search words are kept by the Discover screen, so copy them from the old filter
        InternshipFilter old = (InternshipFilter) requireArguments().getSerializable(ARG_FILTER);
        if (old != null) {
            filter.query = old.query;
        }
        filter.skill = binding.editSkill.getText().toString().trim();
        filter.location = binding.editLocation.getText().toString().trim();
        filter.type = selectedTag(binding.groupType);
        filter.deadlineDays = Integer.parseInt(selectedTag(binding.groupDeadline));
        filter.paidOnly = binding.switchPaid.isChecked();

        if (getParentFragment() instanceof Listener) {
            ((Listener) getParentFragment()).onFilterApplied(filter);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
