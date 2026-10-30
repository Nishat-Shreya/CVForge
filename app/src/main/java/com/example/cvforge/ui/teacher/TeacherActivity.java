package com.example.cvforge.ui.teacher;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.R;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityTeacherBinding;
import com.example.cvforge.ui.auth.LogoutHelper;
import com.example.cvforge.ui.post.PostInternshipActivity;
import com.example.cvforge.util.AnimUtils;

/** Teacher home screen. The real features are added in later checkpoints. */
public class TeacherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTeacherBinding binding = ActivityTeacherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnPost.setOnClickListener(v -> {
            startActivity(new Intent(this, PostInternshipActivity.class));
            AnimUtils.fadeTransition(this);
        });
        binding.btnLogout.setOnClickListener(v -> LogoutHelper.logOut(this));
        AnimUtils.slideUpIn(binding.heroCard, 0);

        new UserRepository().loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                binding.textGreeting.setText(getString(R.string.hello_name, user.getName()));
            }

            @Override
            public void onError(String message) {
                binding.textGreeting.setText(message);
            }
        });
    }
}
