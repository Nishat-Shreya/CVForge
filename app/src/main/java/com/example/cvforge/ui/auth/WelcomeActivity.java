package com.example.cvforge.ui.auth;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.cvforge.R;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.User;
import com.example.cvforge.databinding.ActivityWelcomeBinding;
import com.example.cvforge.databinding.ContentWelcomeBinding;
import com.example.cvforge.databinding.IncludeWelcomeCardBinding;
import com.example.cvforge.ui.RoleRouter;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private ContentWelcomeBinding content;
    private boolean tallScreen;
    private final List<ObjectAnimator> floatAnimators = new ArrayList<>();
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityWelcomeBinding binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        content = binding.content;
        tallScreen = getResources().getBoolean(R.bool.is_tall_screen);

        setUpSystemBars();
        setUpCards();

        routeIfLoggedIn();

        content.btnGoogle.setOnClickListener(v -> signInWithGoogle());
        content.btnEmailSignUp.setOnClickListener(v -> openSignUp());
        content.textLogIn.setOnClickListener(v -> openLogin());
    }

    /**
     * Draws the violet screen behind the status bar and the white sheet behind the
     * gesture bar, then moves the content away from both with the window insets.
     */
    private void setUpSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        int sheetPaddingBottom = content.sheet.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content.contentRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            content.sheet.setPadding(content.sheet.getPaddingLeft(), content.sheet.getPaddingTop(),
                    content.sheet.getPaddingRight(), sheetPaddingBottom + bars.bottom);
            return windowInsets;
        });
    }

    private void setUpCards() {
        fillCard(content.cardMatch, R.drawable.ic_check, R.color.teal,
                R.string.welcome_card_match_title, R.string.welcome_card_match_sub, -2);
        fillCard(content.cardInterview, R.drawable.ic_calendar, R.color.amber,
                R.string.welcome_card_interview_title, R.string.welcome_card_interview_sub, 3);
        fillCard(content.cardVerified, R.drawable.ic_shield_check, R.color.violet,
                R.string.welcome_card_verified_title, R.string.welcome_card_verified_sub, -2);

        // short screens (under 640dp high): hide the third card and the subtitle
        if (!tallScreen) {
            content.cardVerified.welcomeCard.setVisibility(View.GONE);
            content.textSubtitle.setVisibility(View.GONE);
        }
    }

    /** A5: the cards float while the screen is visible. */
    @Override
    protected void onStart() {
        super.onStart();
        addFloat(content.cardMatch.welcomeCard, 0);
        addFloat(content.cardInterview.welcomeCard, 1300);
        if (tallScreen) {
            addFloat(content.cardVerified.welcomeCard, 2600);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (ObjectAnimator animator : floatAnimators) {
            animator.cancel();
        }
        floatAnimators.clear();
    }

    private void addFloat(View card, long offset) {
        ObjectAnimator animator = AnimUtils.floatUpDown(card, offset);
        if (animator != null) {
            floatAnimators.add(animator);
        }
    }

    private void fillCard(IncludeWelcomeCardBinding card, int iconRes, int colorRes, int titleRes, int subRes, float tilt) {
        card.welcomeCard.setRotation(tilt);
        card.cardIcon.setImageResource(iconRes);
        card.cardIcon.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));
        card.cardTitle.setText(titleRes);
        card.cardSub.setText(subRes);
    }

    /** Explicit Intent to the sign up screen, with the chosen role as an extra. */
    private void openSignUp() {
        View chip = content.groupRole.findViewById(content.groupRole.getCheckedChipId());
        Intent intent = new Intent(this, SignUpActivity.class);
        // Student is the default, so the button never crashes when no chip is checked
        intent.putExtra(SignUpActivity.EXTRA_ROLE, chip == null ? User.ROLE_STUDENT : (String) chip.getTag());
        startActivity(intent);
        AnimUtils.fadeTransition(this);
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        AnimUtils.fadeTransition(this);
    }

    /** Already signed in from an earlier visit: skip Welcome and go to the role screen. */
    private void routeIfLoggedIn() {
        if (!userRepository.isLoggedIn()) {
            return;
        }
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                RoleRouter.open(WelcomeActivity.this, user);
            }

            @Override
            public void onError(String message) {
                // stay on Welcome so the user can log in again
            }
        });
    }

    /** The role on the chips is used only if this Google account is new. */
    private void signInWithGoogle() {
        View chip = content.groupRole.findViewById(content.groupRole.getCheckedChipId());
        String role = chip == null ? null : (String) chip.getTag();
        new GoogleSignInHelper(this).start(role, new GoogleSignInHelper.Callback() {
            @Override
            public void onStarted() {
                content.btnGoogle.setEnabled(false);
            }

            @Override
            public void onStopped(String errorMessage) {
                content.btnGoogle.setEnabled(true);
                if (errorMessage != null) {
                    Toast.makeText(WelcomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
