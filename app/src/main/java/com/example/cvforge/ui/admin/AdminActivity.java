package com.example.cvforge.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityAdminBinding;
import com.example.cvforge.ui.auth.WelcomeActivity;
import com.example.cvforge.util.AnimUtils;

import java.util.List;

/** Admin screen: approve Company and Alumni accounts so they get the Verified badge. */
public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private PendingUserAdapter adapter;
    private final UserRepository userRepository = new UserRepository();

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

        binding.btnLogout.setOnClickListener(v -> logOut());
        loadPending();
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

    /** Signs out and goes back to the Welcome screen. */
    private void logOut() {
        userRepository.logout();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        AnimUtils.fadeTransition(this);
    }
}
