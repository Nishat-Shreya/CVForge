package com.example.cvforge.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.cvforge.data.JobRepository;
import com.example.cvforge.data.model.Internship;

import java.util.List;

/**
 * A started Service (W10) that loads the jobs from the websites in the background.
 *
 * Lifecycle: onCreate runs once when the service is created, onStartCommand runs every time
 * someone calls startService, and onDestroy runs after stopSelf. Each step is written to
 * Logcat (tag JobSyncService) so the lifecycle can be seen.
 *
 * Communication: when the work is done the service sends a broadcast. The Discover screen
 * receives it and refreshes its list.
 */
public class JobSyncService extends Service {

    private static final String TAG = "JobSyncService";

    /** Sent when the sync is finished. */
    public static final String ACTION_JOBS_SYNCED = "com.example.cvforge.JOBS_SYNCED";
    /** Number of jobs after the sync. */
    public static final String EXTRA_COUNT = "count";
    /** False when the RSS feed could not be loaded and saved jobs were used. */
    public static final String EXTRA_FEED_OK = "feed_ok";
    public static final String EXTRA_ERROR = "error";

    private JobRepository jobRepository;
    private boolean feedOk;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: the service is created");
        jobRepository = new JobRepository(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: sync starts (startId " + startId + ")");
        feedOk = true;

        jobRepository.loadExternalJobs(new JobRepository.Listener() {
            @Override
            public void onLoaded(List<Internship> jobs) {
                Log.d(TAG, "sync done, " + jobs.size() + " jobs");
                sendResult(jobs.size(), "");
                stopSelf(startId);
            }

            @Override
            public void onError(String message) {
                Log.d(TAG, "sync failed: " + message);
                sendResult(0, message);
                stopSelf(startId);
            }

            @Override
            public void onFeedProblem() {
                feedOk = false;
            }
        });

        // if the system stops the service, it is not started again by itself
        return START_NOT_STICKY;
    }

    /** Tells the app that the sync is finished. The broadcast stays inside this app. */
    private void sendResult(int count, String error) {
        Intent result = new Intent(ACTION_JOBS_SYNCED);
        result.setPackage(getPackageName());
        result.putExtra(EXTRA_COUNT, count);
        result.putExtra(EXTRA_FEED_OK, feedOk);
        result.putExtra(EXTRA_ERROR, error);
        sendBroadcast(result);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: the service is stopped");
        super.onDestroy();
    }

    /** This is a started service, nobody binds to it. */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
