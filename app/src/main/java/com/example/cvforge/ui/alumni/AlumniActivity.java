package com.example.cvforge.ui.alumni;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.R;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityAlumniBinding;
import com.example.cvforge.ui.auth.LogoutHelper;
import com.example.cvforge.ui.chat.ChatsActivity;
import com.example.cvforge.ui.post.ApplicantsActivity;
import com.example.cvforge.ui.teacher.EndorseActivity;
import com.example.cvforge.ui.post.MyPostsActivity;
import com.example.cvforge.ui.post.PosterDashboard;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.NotificationHelper;
import com.example.cvforge.util.NotificationPermission;

/** Alumni home screen: numbers and activity on their opportunities, applicants and endorsements. */
public class AlumniActivity extends AppCompatActivity {

    private NotificationPermission notificationPermission;
    private PosterDashboard dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.createChannel(this);
        notificationPermission = new NotificationPermission(this);
        if (savedInstanceState == null) {
            notificationPermission.requestIfNeeded();
        }
        ActivityAlumniBinding binding = ActivityAlumniBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        dashboard = new PosterDashboard(this, binding.stats, true);

        binding.btnPost.setOnClickListener(v -> {
            startActivity(new Intent(this, MyPostsActivity.class));
            AnimUtils.fadeTransition(this);
        });
        binding.btnApplicants.setOnClickListener(v -> {
            startActivity(new Intent(this, ApplicantsActivity.class));
            AnimUtils.fadeTransition(this);
        });
        binding.btnSlots.setOnClickListener(v -> {
            startActivity(new Intent(this, SlotsActivity.class));
            AnimUtils.fadeTransition(this);
        });
        binding.btnMessages.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatsActivity.class));
            AnimUtils.fadeTransition(this);
        });
        binding.btnEndorse.setOnClickListener(v -> {
            startActivity(new Intent(this, EndorseActivity.class));
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

    /** The numbers are loaded again every time the screen comes back (after endorsing or posting). */
    @Override
    protected void onStart() {
        super.onStart();
        dashboard.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        dashboard.stop();
    }
}
