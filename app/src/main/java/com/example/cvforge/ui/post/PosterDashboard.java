package com.example.cvforge.ui.post;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.cvforge.R;
import com.example.cvforge.data.ApplicationRepository;
import com.example.cvforge.data.EndorsementRepository;
import com.example.cvforge.data.InternshipRepository;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Endorsement;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.databinding.IncludePosterStatsBinding;
import com.example.cvforge.databinding.IncludeStatTileBinding;
import com.example.cvforge.ui.tracker.StatusStyle;
import com.example.cvforge.util.AnimUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The numbers and the latest activity on the Teacher and Alumni dashboards: open posts,
 * applicants, endorsement requests and the endorsements given. Call start() in onStart
 * and stop() in onStop, because the activity list is live.
 */
public class PosterDashboard {

    private static final int RECENT = 4;

    private final Context context;
    private final IncludePosterStatsBinding views;
    private final String uid = new UserRepository().getCurrentUid();
    private final InternshipRepository internshipRepository = new InternshipRepository();
    private final ApplicationRepository applicationRepository = new ApplicationRepository();
    private final EndorsementRepository endorsementRepository = new EndorsementRepository();

    private ListenerRegistration registration;

    /** forAlumni only changes the title of the activity list. */
    public PosterDashboard(Context context, IncludePosterStatsBinding views, boolean forAlumni) {
        this.context = context;
        this.views = views;

        views.textActivityTitle.setText(forAlumni ? R.string.dash_title_alumni : R.string.dash_title_teacher);
        views.textNoActivity.setText(R.string.dash_no_activity);
        setUpTile(views.tileOpen, R.string.stat_open_posts, R.color.sky_tint, R.color.sky_text);
        setUpTile(views.tileApplicants, R.string.stat_applicants, R.color.teal_tint, R.color.teal_text);
        setUpTile(views.tilePending, R.string.stat_pending_endorse, R.color.amber_tint, R.color.amber_text);
        setUpTile(views.tileEndorsed, R.string.stat_endorsed, R.color.coral_tint, R.color.coral_text);
    }

    private void setUpTile(IncludeStatTileBinding tile, int labelRes, int tintRes, int textRes) {
        int text = ContextCompat.getColor(context, textRes);
        tile.textLabel.setText(labelRes);
        tile.textLabel.setTextColor(text);
        tile.textValue.setTextColor(text);
        tile.tileRoot.setBackgroundTintList(ContextCompat.getColorStateList(context, tintRes));
    }

    public void start() {
        internshipRepository.loadByPoster(uid, new InternshipRepository.InternshipListListener() {
            @Override
            public void onLoaded(List<Internship> internships) {
                int open = 0;
                for (Internship internship : internships) {
                    if (internship.isOpen()) {
                        open++;
                    }
                }
                showNumber(views.tileOpen.textValue, open);
            }

            @Override
            public void onError(String message) {
                // the number stays 0
            }
        });

        endorsementRepository.loadPendingForEndorser(uid, countListener(views.tilePending.textValue));
        endorsementRepository.loadApprovedBy(uid, countListener(views.tileEndorsed.textValue));

        registration = applicationRepository.listenByPoster(uid, new ApplicationRepository.ListListener() {
            @Override
            public void onChanged(List<Application> applications) {
                showNumber(views.tileApplicants.textValue, applications.size());
                showRecent(applications);
            }

            @Override
            public void onError(String message) {
                views.textNoActivity.setText(message);
            }
        });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private EndorsementRepository.ListListener countListener(TextView target) {
        return new EndorsementRepository.ListListener() {
            @Override
            public void onLoaded(List<Endorsement> endorsements) {
                showNumber(target, endorsements.size());
            }

            @Override
            public void onError(String message) {
                // the number stays 0
            }
        };
    }

    /** A1 style count-up the first time, plain numbers when the list changes later. */
    private void showNumber(TextView view, int value) {
        if (view.getTag() != null) {
            view.setText(String.valueOf(value));
        } else {
            view.setTag(Boolean.TRUE);
            AnimUtils.countUp(view, value);
        }
    }

    /** The newest changes: "Student · Internship" with the status as text. */
    private void showRecent(List<Application> applications) {
        List<Application> sorted = new ArrayList<>(applications);
        Collections.sort(sorted, (a, b) -> Long.compare(b.lastChangeAt, a.lastChangeAt));

        views.listActivity.removeAllViews();
        views.listActivity.addView(views.textNoActivity);
        views.textNoActivity.setVisibility(sorted.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = 0; i < sorted.size() && i < RECENT; i++) {
            Application application = sorted.get(i);
            TextView row = new TextView(context);
            row.setText(context.getString(R.string.dash_activity_row, application.studentName,
                    application.internshipTitle, context.getString(StatusStyle.labelRes(application.status))));
            row.setTextColor(ContextCompat.getColor(context, R.color.ink));
            row.setTextSize(14);
            row.setMinHeight((int) (40 * context.getResources().getDisplayMetrics().density));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            views.listActivity.addView(row);
            AnimUtils.slideUpIn(row, i);
        }
    }
}
