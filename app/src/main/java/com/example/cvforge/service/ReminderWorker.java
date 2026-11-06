package com.example.cvforge.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cvforge.data.model.Application;
import com.example.cvforge.util.ReminderScheduler;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

/**
 * Background work (W11 WorkManager). It loads the applications of the signed-in student and
 * sets the reminder alarms again. WorkManager runs it every 6 hours, also after a restart.
 * It runs on a background thread, so waiting for Firestore here is fine.
 */
public class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
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
            QuerySnapshot snapshot = Tasks.await(FirebaseFirestore.getInstance()
                    .collection("applications").whereEqualTo("studentId", user.getUid()).get());
            List<Application> applications = snapshot.toObjects(Application.class);
            ReminderScheduler.schedule(getApplicationContext(), applications);
            Log.d(TAG, "reminders updated for " + applications.size() + " applications");
            return Result.success();
        } catch (Exception e) {
            // no internet: try again later
            Log.d(TAG, "could not load applications: " + e.getMessage());
            return Result.retry();
        }
    }
}
