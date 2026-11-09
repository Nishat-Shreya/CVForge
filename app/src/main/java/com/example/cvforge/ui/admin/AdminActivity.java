package com.example.cvforge.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.ai.ScamReview;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityAdminBinding;
import com.example.cvforge.ui.auth.LogoutHelper;

import java.util.List;

/**
 * Admin screen with two tabs: approve Company and Alumni accounts (Verified badge), and
 * review flagged internship posts (rule checks plus an optional AI second opinion).
 */
public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private PendingUserAdapter adapter;
    private FlaggedPostAdapter postAdapter;
    private final UserRepository userRepository = new UserRepository();
    private final InternshipRepository internshipRepository = new InternshipRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(R.string.approve_accounts);
        binding.toolbar.btnBack.setVisibility(View.GONE);

        adapter = new PendingUserAdapter(this::approve);
        binding.listPending.setLayoutManager(new LinearLayoutManager(this));
        binding.listPending.setAdapter(adapter);

        postAdapter = new FlaggedPostAdapter(new FlaggedPostAdapter.Listener() {
            @Override
            public void onAskAi(Internship post) {
                askAi(post);
            }

            @Override
            public void onKeep(Internship post) {
                keepPost(post);
            }

            @Override
            public void onRemove(Internship post) {
                removePost(post);
            }
        });
        binding.listFlagged.setLayoutManager(new LinearLayoutManager(this));
        binding.listFlagged.setAdapter(postAdapter);

        binding.groupTabs.setOnCheckedChangeListener((group, checkedId) -> showTab(checkedId == R.id.chipPosts));
        binding.btnLogout.setOnClickListener(v -> LogoutHelper.logOut(this));
        loadPending();
    }

    private void showTab(boolean posts) {
        binding.listPending.setVisibility(posts ? View.GONE : View.VISIBLE);
        binding.listFlagged.setVisibility(posts ? View.VISIBLE : View.GONE);
        binding.toolbar.textTitle.setText(posts ? R.string.tab_flagged_posts : R.string.approve_accounts);
        if (posts) {
            loadFlagged();
        } else {
            showEmptyState(getString(R.string.nothing_pending), getString(R.string.nothing_pending_message));
        }
    }

    // ---------- flagged posts ----------

    private void loadFlagged() {
        binding.progress.setVisibility(View.VISIBLE);
        internshipRepository.loadFlagged(new InternshipRepository.InternshipListListener() {
            @Override
            public void onLoaded(List<Internship> internships) {
                binding.progress.setVisibility(View.GONE);
                postAdapter.setPosts(internships);
                showPostsEmptyState();
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                binding.empty.textEmptyTitle.setText(R.string.error_loading);
                binding.empty.textEmptyMessage.setText(message);
                binding.empty.emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    /** The AI only gives advice. The admin still decides with Keep or Remove. */
    private void askAi(Internship post) {
        postAdapter.setWaiting(post, true);
        new ScamReview(this).review(post, new ScamReview.Callback() {
            @Override
            public void onResult(ScamReview.Result result) {
                postAdapter.setWaiting(post, false);
                postAdapter.setOpinion(post, result);
            }

            @Override
            public void onError(String message) {
                postAdapter.setWaiting(post, false);
                // the message says why (no internet, limit, service), then the admin decides alone
                Toast.makeText(AdminActivity.this, message + " " + getString(R.string.scam_decide_alone),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void keepPost(Internship post) {
        internshipRepository.clearFlag(post.id, postResult(post, R.string.scam_kept));
    }

    private void removePost(Internship post) {
        internshipRepository.setStatus(post.id, Internship.STATUS_CLOSED, postResult(post, R.string.scam_removed));
    }

    private ResultListener postResult(Internship post, int doneMessage) {
        return new ResultListener() {
            @Override
            public void onSuccess() {
                postAdapter.removePost(post);
                Toast.makeText(AdminActivity.this, doneMessage, Toast.LENGTH_SHORT).show();
                showPostsEmptyState();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void showPostsEmptyState() {
        binding.empty.textEmptyTitle.setText(R.string.nothing_flagged);
        binding.empty.textEmptyMessage.setText(R.string.nothing_flagged_message);
        binding.empty.emptyState.setVisibility(postAdapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadPending() {
        binding.progress.setVisibility(View.VISIBLE);
        userRepository.loadPendingUsers(new UserListListener() {
            @Override
            public void onUsers(List<User> users) {
                binding.progress.setVisibility(View.GONE);
                adapter.setUsers(users);
                showEmptyState(getString(R.string.nothing_pending), getString(R.string.nothing_pending_message));
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                showEmptyState(getString(R.string.error_loading), message);
            }
        });
    }

    private void approve(User user) {
        userRepository.approveUser(user.getUid(), new ResultListener() {
            @Override
            public void onSuccess() {
                adapter.removeUser(user);
                showEmptyState(getString(R.string.nothing_pending), getString(R.string.nothing_pending_message));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** The empty state only shows when the list has no items. */
    private void showEmptyState(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

}
