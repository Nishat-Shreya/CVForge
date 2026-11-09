package com.example.cvforge.ui.discover;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.BookmarkRepository;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.data.model.User;

import java.util.List;

/**
 * Save and Apply for an internship. The detail screen and the swipe cards both use it,
 * so the rules are the same everywhere: apply needs a CV, and with more than one CV version
 * the student picks which one.
 */
public class ApplyFlow {

    /** What happened. Always called on the main thread. */
    public interface Callback {
        void onSuccess();

        /** message is null when the student only cancelled. */
        void onFailure(String message);
    }

    private final AppCompatActivity activity;
    private final UserRepository userRepository = new UserRepository();
    private final ApplicationRepository applicationRepository = new ApplicationRepository();

    public ApplyFlow(AppCompatActivity activity) {
        this.activity = activity;
    }

    /** Saves the internship in the tracker (status "saved") and bookmarks it on the phone. */
    public void save(Internship post, Callback callback) {
        String uid = userRepository.getCurrentUid();
        // the bookmark is also kept on the phone, so the saved list opens offline
        new BookmarkRepository(activity).add(uid, post);
        withStudentName(callback, name -> applicationRepository.save(uid, name, post, new ResultListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(activity, R.string.save_done, Toast.LENGTH_SHORT).show();
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                callback.onFailure(message);
            }
        }));
    }

    /** Apply needs a CV. With more than one version the student picks which one. */
    public void apply(Internship post, Callback callback) {
        String uid = userRepository.getCurrentUid();
        new CvRepository(activity).loadAll(uid, cvs -> {
            if (activity.isDestroyed()) {
                return;
            }
            if (cvs.isEmpty()) {
                Toast.makeText(activity, R.string.need_cv_first, Toast.LENGTH_LONG).show();
                callback.onFailure(null);
            } else if (cvs.size() == 1) {
                sendApplication(post, cvs.get(0), callback);
            } else {
                chooseCv(post, cvs, callback);
            }
        });
    }

    private void chooseCv(Internship post, List<Cv> cvs, Callback callback) {
        String[] titles = new String[cvs.size()];
        for (int i = 0; i < cvs.size(); i++) {
            titles[i] = cvs.get(i).title;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.choose_cv_title)
                .setItems(titles, (dialog, which) -> sendApplication(post, cvs.get(which), callback))
                .setOnCancelListener(dialog -> callback.onFailure(null))
                .show();
    }

    private void sendApplication(Internship post, Cv cv, Callback callback) {
        String uid = userRepository.getCurrentUid();
        // the companies see which skills are verified, so the list is refreshed first
        new com.example.cvforge.data.EndorsementRepository().fillVerifiedSkills(cv, uid, () ->
                sendWithVerifiedSkills(post, cv, callback));
    }

    private void sendWithVerifiedSkills(Internship post, Cv cv, Callback callback) {
        String uid = userRepository.getCurrentUid();
        withStudentName(callback, name -> applicationRepository.apply(uid, name, post, cv, new ResultListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(activity, R.string.apply_done, Toast.LENGTH_SHORT).show();
                // a job from another website is applied for on that website
                if (post.isExternal() && post.url != null) {
                    openLink(post.url);
                }
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                callback.onFailure(message);
            }
        }));
    }

    /** Implicit Intent: opens the link in the browser. */
    public void openLink(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.no_app_for_link, Toast.LENGTH_SHORT).show();
        }
    }

    private interface NameCallback {
        void onName(String name);
    }

    /** The application stores the student's name, so the company can see who applied. */
    private void withStudentName(Callback callback, NameCallback next) {
        userRepository.loadCurrentUser(new UserListener() {
            @Override
            public void onUser(User user) {
                next.onName(user.getName());
            }

            @Override
            public void onError(String message) {
                callback.onFailure(message);
            }
        });
    }
}
