package com.example.cvforge.ui.post;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.ActivityMyPostsBinding;
import com.example.cvforge.util.AnimUtils;

import java.util.List;

/** The poster's own internships. They can edit, close or reopen, and delete each one. */
public class MyPostsActivity extends AppCompatActivity implements PostAdapter.Listener {

    private ActivityMyPostsBinding binding;
    private PostAdapter adapter;
    private final InternshipRepository repository = new InternshipRepository();
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(R.string.my_postings);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        adapter = new PostAdapter(this);
        binding.listPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.listPosts.setAdapter(adapter);

        binding.btnNewPost.setOnClickListener(v -> openForm(null));
    }

    /** Reloads when we come back from the form, so new and edited posts show up. */
    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        binding.progress.setVisibility(View.VISIBLE);
        repository.loadByPoster(userRepository.getCurrentUid(), new InternshipRepository.InternshipListListener() {
            @Override
            public void onLoaded(List<Internship> internships) {
                binding.progress.setVisibility(View.GONE);
                adapter.setPosts(internships);
                showEmptyState(getString(R.string.no_posts_title), getString(R.string.no_posts_message));
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                showEmptyState(getString(R.string.error_loading), message);
            }
        });
    }

    private void showEmptyState(String title, String message) {
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openForm(String postId) {
        Intent intent = new Intent(this, PostInternshipActivity.class);
        if (postId != null) {
            intent.putExtra(PostInternshipActivity.EXTRA_POST_ID, postId);
        }
        startActivity(intent);
        AnimUtils.fadeTransition(this);
    }

    // ---------- actions on one post ----------

    @Override
    public void onEdit(Internship post) {
        openForm(post.id);
    }

    @Override
    public void onToggleStatus(Internship post) {
        String newStatus = post.isOpen() ? Internship.STATUS_CLOSED : Internship.STATUS_OPEN;
        repository.setStatus(post.id, newStatus, new ResultListener() {
            @Override
            public void onSuccess() {
                post.status = newStatus;
                adapter.refresh(post);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MyPostsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDelete(Internship post) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_post_title)
                .setMessage(getString(R.string.delete_post_message, post.title))
                .setPositiveButton(R.string.delete, (dialog, which) -> deletePost(post))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deletePost(Internship post) {
        repository.delete(post.id, new ResultListener() {
            @Override
            public void onSuccess() {
                adapter.remove(post);
                showEmptyState(getString(R.string.no_posts_title), getString(R.string.no_posts_message));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MyPostsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
