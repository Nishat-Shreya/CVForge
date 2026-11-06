package com.example.cvforge.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.ui.auth.WelcomeActivity;
import com.example.cvforge.util.NotificationHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ghosting Detector (feature 13), background work with WorkManager. Once a day it looks for
 * applications where the company has not replied for 7 days (a simple rule on the dates).
 * The student gets a nudge, and tapping it opens the application with a follow-up email draft.
 * Every application is announced only once.
 */
public class GhostingWorker extends Worker {

    private static final String TAG = "GhostingWorker";
    private static final String PREFS = "ghosting";
    private static final String KEY_NOTIFIED = "notified_ids";
    /** At most this many nudges in one day, so the phone is not flooded. */
    private static final int MAX_PER_DAY = 3;

    public GhostingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
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
            nudge(applications);
            return Result.success();
        } catch (Exception e) {
            // no internet: the check runs again tomorrow
            Log.d(TAG, "could not load applications: " + e.getMessage());
            return Result.success();
        }
    }

    private void nudge(List<Application> applications) {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> notified = new HashSet<>(prefs.getStringSet(KEY_NOTIFIED, new HashSet<>()));
        long now = System.currentTimeMillis();

        int sent = 0;
        for (Application application : applications) {
            if (sent >= MAX_PER_DAY || !application.isGhosted(now) || notified.contains(application.id)) {
                continue;
            }
            long days = (now - application.lastChangeAt) / (24L * 60 * 60 * 1000);
            NotificationHelper.show(context, application.id.hashCode(),
                    context.getString(R.string.ghosting_title, days),
                    context.getString(R.string.ghosting_text, application.companyName, application.internshipTitle),
                    openApplicationIntent(context, application.id));
            notified.add(application.id);
            sent++;
        }
        prefs.edit().putStringSet(KEY_NOTIFIED, notified).apply();
        Log.d(TAG, sent + " nudges sent");
    }

    /** The app opens at the application. "target" is read by the home screen after the login check. */
    private PendingIntent openApplicationIntent(Context context, String applicationId) {
        Intent open = new Intent(context, WelcomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra(CvForgeMessagingService.EXTRA_TARGET, "application:" + applicationId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, applicationId.hashCode(), open, flags);
    }
}
