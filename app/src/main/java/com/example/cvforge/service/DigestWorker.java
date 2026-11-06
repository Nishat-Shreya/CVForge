package com.example.cvforge.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.ui.auth.WelcomeActivity;
import com.example.cvforge.util.DigestPlanner;
import com.example.cvforge.util.NotificationHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.TimeZone;

/**
 * Background work (W11 WorkManager): every morning at about 8:00 it counts the deadlines and
 * interviews of today and shows one notification, for example "2 deadlines and 1 interview today".
 * Nothing is shown on a day without deadlines and interviews.
 */
public class DigestWorker extends Worker {

    private static final String TAG = "DigestWorker";
    private static final int NOTIFICATION_ID = 7001;

    public DigestWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.success();
        }
        try {
            List<Application> applications = Tasks.await(FirebaseFirestore.getInstance()
                    .collection("applications").whereEqualTo("studentId", user.getUid()).get())
                    .toObjects(Application.class);

            DigestPlanner.Counts counts = DigestPlanner.countToday(applications,
                    System.currentTimeMillis(), TimeZone.getDefault());
            Log.d(TAG, "today: " + counts.deadlines + " deadlines, " + counts.interviews + " interviews");

            if (!counts.isEmpty()) {
                NotificationHelper.show(getApplicationContext(), NOTIFICATION_ID,
                        getApplicationContext().getString(R.string.digest_title),
                        digestText(counts), openAppIntent());
            }
            return Result.success();
        } catch (Exception e) {
            // no internet: the digest of this day is skipped
            Log.d(TAG, "could not load applications: " + e.getMessage());
            return Result.success();
        }
    }

    /** "2 deadlines and 1 interview today". A part with 0 is left out. */
    private String digestText(DigestPlanner.Counts counts) {
        Context context = getApplicationContext();
        String deadlines = context.getResources().getQuantityString(
                R.plurals.digest_deadlines, counts.deadlines, counts.deadlines);
        String interviews = context.getResources().getQuantityString(
                R.plurals.digest_interviews, counts.interviews, counts.interviews);

        if (counts.deadlines > 0 && counts.interviews > 0) {
            return context.getString(R.string.digest_both, deadlines, interviews);
        }
        return context.getString(R.string.digest_one, counts.deadlines > 0 ? deadlines : interviews);
    }

    private PendingIntent openAppIntent() {
        Intent open = new Intent(getApplicationContext(), WelcomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(getApplicationContext(), 2, open, flags);
    }
}
