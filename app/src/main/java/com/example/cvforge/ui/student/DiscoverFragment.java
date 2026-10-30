package com.example.cvforge.ui.student;

import android.os.Bundle;
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
import com.example.cvforge.ui.discover.InternshipAdapter;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Discover tab: the list of open internships (feature 4). Quick filters: For you (everything),
 * Remote and Paid. The full filter sheet, search and details come in later checkpoints.
 */
public class DiscoverFragment extends Fragment {

    private FragmentDiscoverBinding binding;
    private InternshipAdapter adapter;
    private final InternshipRepository repository = new InternshipRepository();

    /** All open internships from Firestore. The quick filters work on this list. */
    private List<Internship> all = new ArrayList<>();
    private boolean remoteOnly;
    private boolean paidOnly;

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

        binding.chipForYou.setOnClickListener(v -> {
            remoteOnly = false;
            paidOnly = false;
            applyFilters();
        });
        binding.chipRemote.setOnClickListener(v -> {
            remoteOnly = !remoteOnly;
            applyFilters();
        });
        binding.chipPaidOnly.setOnClickListener(v -> {
            paidOnly = !paidOnly;
            applyFilters();
        });
        showChips();

        // A4: the blocks slide up one after the other
        AnimUtils.slideUpIn(binding.rowTitle, 0);
        AnimUtils.slideUpIn(binding.scrollChips, 1);
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
                applyFilters();
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

    /** Shows the internships that pass the quick filters. */
    private void applyFilters() {
        List<Internship> shown = new ArrayList<>();
        for (Internship post : all) {
            boolean remoteOk = !remoteOnly || Internship.TYPE_REMOTE.equals(post.type);
            boolean paidOk = !paidOnly || post.stipend > 0;
            if (remoteOk && paidOk) {
                shown.add(post);
            }
        }
        adapter.setItems(shown);
        showChips();

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
        styleChip(binding.chipForYou, !remoteOnly && !paidOnly);
        styleChip(binding.chipRemote, remoteOnly);
        styleChip(binding.chipPaidOnly, paidOnly);
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
