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
import androidx.appcompat.app.AppCompatActivity;
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
import com.example.cvforge.ui.discover.ApplyFlow;
import com.example.cvforge.ui.discover.BookmarksActivity;
import com.example.cvforge.ui.discover.FilterSheet;
import com.example.cvforge.ui.discover.InternshipAdapter;
import com.example.cvforge.ui.discover.InternshipDetailActivity;
import com.example.cvforge.ui.discover.InternshipFilter;
import com.example.cvforge.ui.discover.SwipeCardView;
import com.example.cvforge.util.AnimUtils;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Discover tab: the open internships as swipe cards or as a list, with search and filters. */
public class DiscoverFragment extends Fragment implements FilterSheet.Listener {

    private FragmentDiscoverBinding binding;
    private InternshipAdapter adapter;
    private final InternshipRepository repository = new InternshipRepository();

    private JobRepository jobRepository;

    /** Internships posted in CVForge, and real jobs from other websites. */
    private List<Internship> cvforgeInternships = new ArrayList<>();
    private List<Internship> externalJobs = new ArrayList<>();
    private List<Internship> allInternships = new ArrayList<>();
    /** What the student searched and filtered for. */
    private InternshipFilter filter = new InternshipFilter();

    // swipe cards
    private boolean cardMode = true;
    private List<Internship> deck = new ArrayList<>();
    private final Set<String> skippedIds = new HashSet<>();
    private List<String> studentSkills = new ArrayList<>();
    /** True while a card is flying out or an application is being sent. */
    private boolean busy;
    /** The id of the internship on the big card right now. */
    private String shownCardId;
    private ApplyFlow applyFlow;

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
        applyFlow = new ApplyFlow((AppCompatActivity) requireActivity());
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

        binding.btnView.setOnClickListener(v -> setCardMode(!cardMode));
        binding.btnSkip.setOnClickListener(v -> skipCard());
        binding.btnApplyCard.setOnClickListener(v -> applyCard());
        binding.btnSaveCard.setOnClickListener(v -> saveCard());
        binding.empty.emptyState.setOnClickListener(v -> {
            skippedIds.clear();
            applyFilter();
        });
        binding.swipeCard.setListener(new SwipeCardView.Listener() {
            @Override
            public void onSwipeLeft() {
                skipCard();
            }

            @Override
            public void onSwipeRight() {
                applyCard();
            }

            @Override
            public void onCardClick() {
                if (!deck.isEmpty()) {
                    openDetails(deck.get(0));
                }
            }
        });

        styleChips();
        setCardMode(requireContext().getSharedPreferences("discover", Context.MODE_PRIVATE)
                .getBoolean("card_mode", true));
        AnimUtils.slideUpIn(binding.rowTitle, 0);
        AnimUtils.slideUpIn(binding.layoutSearch, 1);
        AnimUtils.slideUpIn(binding.scrollChips, 2);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStudentSkills();
        loadInternships();
    }

    /** The skills of the newest CV version, for the match rings. */
    private void loadStudentSkills() {
        new CvRepository(requireContext()).loadAll(new UserRepository().getCurrentUid(), cvs -> {
            if (binding != null) {
                studentSkills = cvs.isEmpty() ? new ArrayList<>() : cvs.get(0).skills;
                adapter.setStudentSkills(studentSkills);
                if (cardMode && !deck.isEmpty() && !busy) {
                    shownCardId = deck.get(0).id;
                    binding.swipeCard.bind(deck.get(0), studentSkills);
                }
            }
        });
    }

    private void loadInternships() {
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
        allInternships = new ArrayList<>(cvforgeInternships);
        allInternships.addAll(externalJobs);
        applyFilter();
    }

    // ---------- filters ----------

    /** Called by the filter sheet when the student taps Apply. */
    @Override
    public void onFilterApplied(InternshipFilter newFilter) {
        filter = newFilter;
        styleChips();
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
        styleChips();
        applyFilter();
    }

    private void togglePaid() {
        filter.paidOnly = !filter.paidOnly;
        styleChips();
        applyFilter();
    }

    /** Shows only the internships that fit the search and the filters. */
    private void applyFilter() {
        long now = System.currentTimeMillis();
        List<Internship> shown = new ArrayList<>();
        for (Internship internship : allInternships) {
            if (filter.matches(internship, now)) {
                shown.add(internship);
            }
        }
        adapter.setItems(shown);

        // the card deck: the same internships, without the ones the student already skipped
        deck = new ArrayList<>();
        for (Internship internship : shown) {
            if (!skippedIds.contains(internship.id)) {
                deck.add(internship);
            }
        }

        if (cardMode) {
            showTopCard();
        } else if (shown.isEmpty()) {
            boolean noInternshipsAtAll = allInternships.isEmpty();
            showEmptyState(
                    getString(noInternshipsAtAll ? R.string.no_internships_title : R.string.no_match_title),
                    getString(noInternshipsAtAll ? R.string.no_internships_message : R.string.no_match_message));
        } else {
            binding.empty.emptyState.setVisibility(View.GONE);
        }
    }

    // ---------- swipe cards (A6) ----------

    /** Shows the first internship of the deck as the big card, or a message when the deck is empty. */
    private void showTopCard() {
        busy = false;
        if (deck.isEmpty()) {
            binding.areaSwipe.setVisibility(View.GONE);
            boolean noInternshipsAtAll = allInternships.isEmpty();
            showEmptyState(
                    getString(noInternshipsAtAll ? R.string.no_internships_title : R.string.deck_empty_title),
                    getString(noInternshipsAtAll ? R.string.no_internships_message : R.string.deck_empty_message));
            return;
        }
        binding.empty.emptyState.setVisibility(View.GONE);
        binding.areaSwipe.setVisibility(View.VISIBLE);
        // a refresh of the list must not restart the card that is already shown
        Internship top = deck.get(0);
        if (!top.id.equals(shownCardId)) {
            shownCardId = top.id;
            binding.swipeCard.bind(top, studentSkills);
        }
    }

    /** The card has left the screen: take it out of the deck and show the next one. */
    private void cardGone(boolean skipped) {
        Internship gone = deck.remove(0);
        shownCardId = null;
        if (skipped) {
            skippedIds.add(gone.id);
        }
        showTopCard();
    }

    private void skipCard() {
        if (busy || deck.isEmpty()) {
            return;
        }
        busy = true;
        binding.swipeCard.flyOut(SwipeCardView.LEFT, () -> cardGone(true));
    }

    /** Swipe right and the Apply button: the card only leaves when the application was sent. */
    private void applyCard() {
        if (busy || deck.isEmpty()) {
            return;
        }
        busy = true;
        applyFlow.apply(deck.get(0), new ApplyFlow.Callback() {
            @Override
            public void onSuccess() {
                if (binding != null) {
                    binding.swipeCard.flyOut(SwipeCardView.RIGHT, () -> cardGone(false));
                }
            }

            @Override
            public void onFailure(String message) {
                busy = false;
                if (binding != null) {
                    binding.swipeCard.springBack();
                    if (message != null) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void saveCard() {
        if (busy || deck.isEmpty()) {
            return;
        }
        busy = true;
        applyFlow.save(deck.get(0), new ApplyFlow.Callback() {
            @Override
            public void onSuccess() {
                if (binding != null) {
                    binding.swipeCard.flyOut(SwipeCardView.UP, () -> cardGone(false));
                }
            }

            @Override
            public void onFailure(String message) {
                busy = false;
            }
        });
    }

    /** Switches between the swipe cards and the list. */
    private void setCardMode(boolean cards) {
        cardMode = cards;
        requireContext().getSharedPreferences("discover", Context.MODE_PRIVATE)
                .edit().putBoolean("card_mode", cards).apply();

        binding.btnView.setIconResource(cards ? R.drawable.ic_list : R.drawable.ic_cards);
        binding.btnView.setContentDescription(getString(cards ? R.string.view_list : R.string.view_cards));
        // the search box and the quick chips are for the list
        int listVisibility = cards ? View.GONE : View.VISIBLE;
        binding.layoutSearch.setVisibility(listVisibility);
        binding.scrollChips.setVisibility(listVisibility);
        binding.listInternships.setVisibility(listVisibility);
        binding.areaSwipe.setVisibility(cards ? View.VISIBLE : View.GONE);
        binding.empty.emptyState.setVisibility(View.GONE);
        applyFilter();
    }

    private void showEmptyState(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(View.VISIBLE);
    }

    /** A selected chip is solid violet, the others have the soft violet tint. */
    private void styleChips() {
        styleChip(binding.chipForYou, filter.sheetFilterCount() == 0);
        styleChip(binding.chipRemote, Internship.TYPE_REMOTE.equals(filter.type));
        styleChip(binding.chipPaidOnly, filter.paidOnly);
    }

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
