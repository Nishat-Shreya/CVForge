package com.example.cvforge.ui.discover;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.BookmarkRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.ActivityBookmarksBinding;
import com.example.cvforge.util.AnimUtils;

/** The internships the student bookmarked. They are read through our own Content Provider. */
public class BookmarksActivity extends AppCompatActivity {

    private ActivityBookmarksBinding binding;
    private InternshipAdapter adapter;
    private BookmarkRepository repository;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookmarksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new BookmarkRepository(this);
        uid = new UserRepository().getCurrentUid();

        binding.toolbar.textTitle.setText(R.string.saved_internships);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        adapter = new InternshipAdapter(this::openDetails);
        adapter.setOnLongClickListener(this::removeBookmark);
        binding.listBookmarks.setLayoutManager(new LinearLayoutManager(this));
        binding.listBookmarks.setAdapter(adapter);
    }

    /** Reloads every time the screen is shown, so a bookmark added on the detail screen appears. */
    @Override
    protected void onResume() {
        super.onResume();
        repository.loadAll(uid, internships -> {
            if (isDestroyed()) {
                return;
            }
            adapter.setItems(internships);
            showEmptyState();
        });
    }

    private void openDetails(Internship internship) {
        Intent intent = new Intent(this, InternshipDetailActivity.class);
        intent.putExtra(InternshipDetailActivity.EXTRA_INTERNSHIP_ID, internship.id);
        startActivity(intent);
        AnimUtils.fadeTransition(this);
    }

    /** Long press removes the bookmark. */
    private void removeBookmark(Internship internship) {
        repository.remove(uid, internship.id, () -> {
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
            onResume();
        });
    }

    private void showEmptyState() {
        binding.empty.textEmptyTitle.setText(R.string.no_bookmarks_title);
        binding.empty.textEmptyMessage.setText(R.string.no_bookmarks_message);
        binding.empty.emptyState.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
