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
import com.example.cvforge.databinding.ActivityLoginBinding;
import com.example.cvforge.ui.RoleRouter;
import com.example.cvforge.util.AnimUtils;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.textTitle.setText(R.string.log_in);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.btnLogin.setOnClickListener(v -> logIn());
        binding.btnGoogle.setOnClickListener(v -> signInWithGoogle());

        AnimUtils.slideUpIn(binding.formContainer, 0);
    }

    private void logIn() {
        String email = binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText().toString();

        if (!isInputValid(email, password)) {
            return;
        }

        setLoading(true);
        userRepository.login(email, password, new ResultListener() {
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

    /** A new Google account has no role yet here, so the helper asks for it. */
    private void signInWithGoogle() {
        binding.textError.setVisibility(View.GONE);
        new GoogleSignInHelper(this).start(null, new GoogleSignInHelper.Callback() {
            @Override
            public void onStarted() {
                binding.btnGoogle.setEnabled(false);
            }

            @Override
            public void onStopped(String errorMessage) {
                binding.btnGoogle.setEnabled(true);
                if (errorMessage != null) {
                    showError(errorMessage);
                }
            }
        });
    }

    /** Reads the role from Firestore, then RoleRouter opens the right screen. */
    private void openHomeForRole() {
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                RoleRouter.open(LoginActivity.this, user);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private boolean isInputValid(String email, String password) {
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        binding.textError.setVisibility(View.GONE);

        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setError(getString(R.string.error_email));
            valid = false;
        }
        if (password.isEmpty()) {
            binding.layoutPassword.setError(getString(R.string.error_password_empty));
            valid = false;
        }
        return valid;
    }

    private void showError(String message) {
        setLoading(false);
        binding.textError.setText(message);
        binding.textError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? R.string.logging_in : R.string.log_in);
    }
}
