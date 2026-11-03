package com.example.cvforge.ui.student;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.JobRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.FragmentDiscoverBinding;
import com.example.cvforge.service.JobSyncService;
import com.example.cvforge.ui.discover.BookmarksActivity;
import com.example.cvforge.ui.discover.FilterSheet;
import com.example.cvforge.ui.discover.InternshipAdapter;
import com.example.cvforge.ui.discover.InternshipDetailActivity;
import com.example.cvforge.ui.discover.InternshipFilter;
import com.example.cvforge.util.AnimUtils;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/** Discover tab: the list of open internships (feature 4), with search and filters. */
public class DiscoverFragment extends Fragment implements FilterSheet.Listener {

    private FragmentDiscoverBinding binding;
    private InternshipAdapter adapter;
    private final InternshipRepository repository = new InternshipRepository();

    private JobRepository jobRepository;

    /** Internships posted in CVForge, and real jobs from other websites. */
    private List<Internship> cvforgeInternships = new ArrayList<>();
    private List<Internship> externalJobs = new ArrayList<>();
    /** Both lists together. The search and the filters work on this list. */
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

        jobRepository = new JobRepository(requireContext());
        adapter = new InternshipAdapter(this::openDetails);
        binding.listInternships.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listInternships.setAdapter(adapter);

        binding.chipForYou.setOnClickListener(v -> clearSheetFilters());
        binding.chipRemote.setOnClickListener(v -> toggleRemote());
        binding.chipPaidOnly.setOnClickListener(v -> togglePaid());
        binding.btnFilter.setOnClickListener(v -> FilterSheet.newInstance(filter).show(getChildFragmentManager(), "filter"));
        binding.btnBookmarks.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), BookmarksActivity.class));
            AnimUtils.fadeTransition(requireActivity());
        });

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
        loadStudentSkills();
        load();
    }

    /** The skills of the newest CV version, for the match rings. */
    private void loadStudentSkills() {
        new CvRepository(requireContext()).loadAll(new UserRepository().getCurrentUid(), cvs -> {
            if (binding != null) {
                adapter.setStudentSkills(cvs.isEmpty() ? new ArrayList<>() : cvs.get(0).skills);
            }
        });
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
                cvforgeInternships = internships;
                combine();
                loadExternalJobs();
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

    /**
     * Shows the saved jobs from Room at once, then asks JobSyncService to refresh them in the
     * background. The service sends a broadcast when it is done (see jobsSyncedReceiver).
     */
    private void loadExternalJobs() {
        showCachedJobs();
        requireContext().startService(new Intent(requireContext(), JobSyncService.class));
    }

    private void showCachedJobs() {
        jobRepository.loadCachedJobs(jobs -> {
            if (binding == null) {
                return;
            }
            externalJobs = jobs;
            combine();
        });
    }

    /**
     * Component communication (W10): receives the broadcast of JobSyncService.
     * It is registered in onStart and unregistered in onStop, so it only listens while the
     * screen is visible.
     */
    private final BroadcastReceiver jobsSyncedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (binding == null) {
                return;
            }
            showCachedJobs();
            if (!intent.getBooleanExtra(JobSyncService.EXTRA_FEED_OK, true)) {
                Toast.makeText(requireContext(), R.string.feed_problem, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        requireContext().registerReceiver(jobsSyncedReceiver, new IntentFilter(JobSyncService.ACTION_JOBS_SYNCED));
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(jobsSyncedReceiver);
    }

    private void combine() {
        all = new ArrayList<>(cvforgeInternships);
        all.addAll(externalJobs);
        applyFilter();
    }

    /** Explicit Intent: opens the detail screen and passes the internship id. */
    private void openDetails(Internship internship) {
        Intent intent = new Intent(requireContext(), InternshipDetailActivity.class);
        intent.putExtra(InternshipDetailActivity.EXTRA_INTERNSHIP_ID, internship.id);
        // a job from another website is not in Firestore, so the whole job travels with the Intent
        if (internship.isExternal()) {
            intent.putExtra(InternshipDetailActivity.EXTRA_INTERNSHIP_JSON, new Gson().toJson(internship));
        }
        startActivity(intent);
        AnimUtils.fadeTransition(requireActivity());
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
