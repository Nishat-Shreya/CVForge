package com.example.cvforge.ui.student;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.FragmentDiscoverBinding;
import com.example.cvforge.ui.discover.FilterSheet;
import com.example.cvforge.ui.discover.InternshipAdapter;
import com.example.cvforge.ui.discover.InternshipFilter;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Discover tab: the list of open internships (feature 4), with search and filters.
 * The details screen comes in a later checkpoint.
 */
public class DiscoverFragment extends Fragment implements FilterSheet.Listener {

    private FragmentDiscoverBinding binding;
    private InternshipAdapter adapter;
    private final InternshipRepository repository = new InternshipRepository();

    /** All open internships from Firestore. The search and the filters work on this list. */
    private List<Internship> all = new ArrayList<>();
    /** What the student searched and filtered for. */
    private InternshipFilter filter = new InternshipFilter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // tapping a card opens the details in a later checkpoint
        adapter = new InternshipAdapter(internship -> { });
        binding.listInternships.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listInternships.setAdapter(adapter);

        binding.chipForYou.setOnClickListener(v -> clearSheetFilters());
        binding.chipRemote.setOnClickListener(v -> toggleRemote());
        binding.chipPaidOnly.setOnClickListener(v -> togglePaid());
        binding.btnFilter.setOnClickListener(v -> FilterSheet.newInstance(filter).show(getChildFragmentManager(), "filter"));

        // the list changes while the student types
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter.query = s.toString().trim();
                applyFilter();
            }
        });
        showChips();

        // A4: the blocks slide up one after the other
        AnimUtils.slideUpIn(binding.rowTitle, 0);
        AnimUtils.slideUpIn(binding.layoutSearch, 1);
        AnimUtils.slideUpIn(binding.scrollChips, 2);
    }

    /** Loads the list every time the tab is shown, so new posts appear. */
    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.empty.emptyState.setVisibility(View.GONE);
        repository.loadOpen(new InternshipRepository.InternshipListListener() {
            @Override
            public void onLoaded(List<Internship> internships) {
                if (binding == null) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                all = internships;
                applyFilter();
            }

            @Override
            public void onError(String message) {
                if (binding == null) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                showEmptyState(getString(R.string.error_loading), message);
            }
        });
    }

    // ---------- search and filters ----------

    /** Called by the filter sheet when the student taps Apply. */
    @Override
    public void onFilterApplied(InternshipFilter newFilter) {
        filter = newFilter;
        showChips();
        applyFilter();
    }

    /** "For you" removes the sheet filters but keeps the search words. */
    private void clearSheetFilters() {
        InternshipFilter cleared = new InternshipFilter();
        cleared.query = filter.query;
        onFilterApplied(cleared);
    }

    private void toggleRemote() {
        filter.type = Internship.TYPE_REMOTE.equals(filter.type) ? "" : Internship.TYPE_REMOTE;
        showChips();
        applyFilter();
    }

    private void togglePaid() {
        filter.paidOnly = !filter.paidOnly;
        showChips();
        applyFilter();
    }

    /** Shows only the internships that fit the search and the filters. */
    private void applyFilter() {
        long now = System.currentTimeMillis();
        List<Internship> shown = new ArrayList<>();
        for (Internship post : all) {
            if (filter.matches(post, now)) {
                shown.add(post);
            }
        }
        adapter.setItems(shown);

        if (shown.isEmpty()) {
            boolean noInternshipsAtAll = all.isEmpty();
            showEmptyState(
                    getString(noInternshipsAtAll ? R.string.no_internships_title : R.string.no_match_title),
                    getString(noInternshipsAtAll ? R.string.no_internships_message : R.string.no_match_message));
        } else {
            binding.empty.emptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(View.VISIBLE);
    }

    // ---------- quick filter chips ----------

    private void showChips() {
        styleChip(binding.chipForYou, filter.sheetFilterCount() == 0);
        styleChip(binding.chipRemote, Internship.TYPE_REMOTE.equals(filter.type));
        styleChip(binding.chipPaidOnly, filter.paidOnly);
    }

    /** A selected chip is solid violet, the others have the soft violet tint. */
    private void styleChip(TextView chip, boolean selected) {
        int background = selected ? R.color.violet : R.color.violet_tint;
        int text = selected ? R.color.white : R.color.violet_text;
        chip.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), background));
        chip.setTextColor(ContextCompat.getColor(requireContext(), text));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
