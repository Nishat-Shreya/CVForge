package com.example.cvforge.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.R;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.databinding.ActivityRolePlaceholderBinding;
import com.example.cvforge.util.AnimUtils;

/**
 * Temporary home screen for every role. It shows who is signed in and has a Log out button.
 * The real screens for Student, Company, Teacher and Alumni replace it in checkpoint 12.
 */
public class RolePlaceholderActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "role";
    public static final String EXTRA_NAME = "name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRolePlaceholderBinding binding = ActivityRolePlaceholderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String name = getIntent().getStringExtra(EXTRA_NAME);
        String role = getIntent().getStringExtra(EXTRA_ROLE);
        binding.textWelcome.setText(getString(R.string.hello_name, name));
        binding.textRole.setText(getString(R.string.signed_in_as, role));

        binding.btnLogout.setOnClickListener(v -> {
            new UserRepository().logout();
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            AnimUtils.fadeTransition(this);
        });
    }
}
