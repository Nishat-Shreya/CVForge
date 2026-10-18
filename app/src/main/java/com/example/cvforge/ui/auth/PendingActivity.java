package com.example.cvforge.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.R;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityPendingBinding;
import com.example.cvforge.ui.RoleRouter;
import com.example.cvforge.util.AnimUtils;

/** Shown to Company and Alumni accounts until an admin approves them. */
public class PendingActivity extends AppCompatActivity {

    private ActivityPendingBinding binding;
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnCheckAgain.setOnClickListener(v -> checkApproval());
        binding.btnLogout.setOnClickListener(v -> logOut());

        AnimUtils.slideUpIn(binding.content, 0);
    }

    /** Signs out and goes back to the Welcome screen. */
    private void logOut() {
        userRepository.logout();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        AnimUtils.fadeTransition(this);
    }

    /** Reads the user again. If an admin approved the account, RoleRouter opens the real screen. */
    private void checkApproval() {
        binding.btnCheckAgain.setEnabled(false);
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                binding.btnCheckAgain.setEnabled(true);
                if (user.isVerified()) {
                    RoleRouter.open(PendingActivity.this, user);
                } else {
                    Toast.makeText(PendingActivity.this, R.string.still_pending, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                binding.btnCheckAgain.setEnabled(true);
                Toast.makeText(PendingActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
