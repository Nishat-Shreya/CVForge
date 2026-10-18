package com.example.cvforge.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.R;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivitySignUpBinding;
import com.example.cvforge.ui.RoleRouter;
import com.example.cvforge.util.AnimUtils;
import com.google.android.material.chip.Chip;

public class SignUpActivity extends AppCompatActivity {

    /** Role chosen on the Welcome screen, passed with an explicit Intent. */
    public static final String EXTRA_ROLE = "role";

    private ActivitySignUpBinding binding;
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(R.string.create_account);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        selectRoleChip(getIntent().getStringExtra(EXTRA_ROLE));
        updateApprovalNote();
        binding.groupRole.setOnCheckedStateChangeListener((group, ids) -> updateApprovalNote());
        binding.btnCreate.setOnClickListener(v -> createAccount());

        AnimUtils.slideUpIn(binding.formContainer, 0);
    }

    private void selectRoleChip(String role) {
        for (int i = 0; i < binding.groupRole.getChildCount(); i++) {
            Chip chip = (Chip) binding.groupRole.getChildAt(i);
            if (chip.getTag().equals(role)) {
                chip.setChecked(true);
                return;
            }
        }
        binding.chipStudent.setChecked(true);
    }

    private String getSelectedRole() {
        int id = binding.groupRole.getCheckedChipId();
        View chip = binding.groupRole.findViewById(id);
        return chip == null ? User.ROLE_STUDENT : (String) chip.getTag();
    }

    private void updateApprovalNote() {
        boolean needsApproval = User.needsApproval(getSelectedRole());
        binding.textApprovalNote.setVisibility(needsApproval ? View.VISIBLE : View.GONE);
    }

    private void createAccount() {
        String name = binding.editName.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText().toString();

        if (!isInputValid(name, email, password)) {
            return;
        }

        setLoading(true);
        userRepository.signUp(name, email, password, getSelectedRole(), new ResultListener() {
            @Override
            public void onSuccess() {
                openHomeForRole();
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void openHomeForRole() {
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                RoleRouter.open(SignUpActivity.this, user);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void showError(String message) {
        setLoading(false);
        binding.textError.setText(message);
        binding.textError.setVisibility(View.VISIBLE);
    }

    private boolean isInputValid(String name, String email, String password) {
        binding.layoutName.setError(null);
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        binding.textError.setVisibility(View.GONE);

        boolean valid = true;
        if (name.isEmpty()) {
            binding.layoutName.setError(getString(R.string.error_name));
            valid = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setError(getString(R.string.error_email));
            valid = false;
        }
        if (password.length() < 6) {
            binding.layoutPassword.setError(getString(R.string.error_password));
            valid = false;
        }
        return valid;
    }

    private void setLoading(boolean loading) {
        binding.btnCreate.setEnabled(!loading);
        binding.btnCreate.setText(loading ? R.string.creating_account : R.string.create_account);
    }
}
