package com.example.cvforge.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.cvforge.data.local.AppDatabase;
import com.example.cvforge.data.local.JobDao;
import com.example.cvforge.data.local.JobEntity;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.network.ApiClient;
import com.example.cvforge.network.ArbeitnowApi;
import com.example.cvforge.network.JobMapper;
import com.example.cvforge.network.RemotiveApi;
import com.example.cvforge.network.model.ArbeitnowResponse;
import com.example.cvforge.network.model.RemotiveResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Real jobs from other websites (Remotive and Arbeitnow), saved in Room.
 * Fresh jobs in Room are used as they are. Remotive asks apps not to call its API too often.
 * If there is no internet, the older jobs in Room are shown, so the list also opens offline.
 */
public class JobRepository {

    /** Callback with the jobs. Runs on the main thread. */
    public interface Listener {
        void onLoaded(List<Internship> jobs);

        void onError(String message);
    }

    /** Callback with one job from the cache, or null if it is not there. Runs on the main thread. */
    public interface JobListener {
        void onLoaded(Internship job);
    }

    /** Callback with a list of jobs from the cache. Runs on the main thread. */
    public interface CacheListener {
        void onLoaded(List<Internship> jobs);
    }

    /** The jobs in Room count as fresh for 6 hours. */
    private static final long FRESH_MS = 6L * 60 * 60 * 1000;

    private final JobDao jobDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final RemotiveApi remotiveApi = ApiClient.create(RemotiveApi.BASE_URL, RemotiveApi.class);
    private final ArbeitnowApi arbeitnowApi = ApiClient.create(ArbeitnowApi.BASE_URL, ArbeitnowApi.class);

    // results of the two web calls (both callbacks run on the main thread, so no locking is needed)
    private final List<Internship> collected = new ArrayList<>();
    private int finishedCalls;
    private int failedCalls;
    private String lastError = "";

    public JobRepository(Context context) {
        jobDao = AppDatabase.getInstance(context).jobDao();
    }

    /** Loads the jobs from Room when they are fresh, otherwise from the websites. */
    public void loadExternalJobs(Listener listener) {
        AppDatabase.databaseExecutor.execute(() -> {
            List<JobEntity> cached = jobDao.getAll();
            long now = System.currentTimeMillis();
            boolean fresh = !cached.isEmpty() && now - cached.get(0).fetchedAt < FRESH_MS;
            if (fresh) {
                List<Internship> jobs = toInternships(cached);
                mainHandler.post(() -> listener.onLoaded(jobs));
            } else {
                mainHandler.post(() -> loadFromWeb(cached, listener));
            }
        });
    }

    /** All jobs that are saved in Room, however old they are. Used to show something quickly. */
    public void loadCachedJobs(CacheListener listener) {
        AppDatabase.databaseExecutor.execute(() -> {
            List<Internship> jobs = toInternships(jobDao.getAll());
            mainHandler.post(() -> listener.onLoaded(jobs));
        });
    }

    /** One job from the cache. The detail screen uses this, because these jobs are not in Firestore. */
    public void loadCachedById(String id, JobListener listener) {
        AppDatabase.databaseExecutor.execute(() -> {
            JobEntity entity = jobDao.getById(id);
            Internship job = entity == null ? null : entity.toInternship();
            mainHandler.post(() -> listener.onLoaded(job));
        });
    }

    // ---------- from the websites ----------

    private void loadFromWeb(List<JobEntity> oldCache, Listener listener) {
        long now = System.currentTimeMillis();
        collected.clear();
        finishedCalls = 0;
        failedCalls = 0;

        remotiveApi.getJobs("intern", 30).enqueue(new Callback<RemotiveResponse>() {
            @Override
            public void onResponse(Call<RemotiveResponse> call, Response<RemotiveResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (RemotiveResponse.Job job : response.body().jobs) {
                        collected.add(JobMapper.fromRemotive(job, now));
                    }
                    callFinished(false, "", oldCache, listener);
                } else {
                    callFinished(true, "Remotive error " + response.code(), oldCache, listener);
                }
            }

            @Override
            public void onFailure(Call<RemotiveResponse> call, Throwable t) {
                callFinished(true, String.valueOf(t.getMessage()), oldCache, listener);
            }
        });

        arbeitnowApi.getJobs().enqueue(new Callback<ArbeitnowResponse>() {
            @Override
            public void onResponse(Call<ArbeitnowResponse> call, Response<ArbeitnowResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (ArbeitnowResponse.Job job : response.body().data) {
                        if (JobMapper.looksLikeInternship(job.title)) {
                            collected.add(JobMapper.fromArbeitnow(job, now));
                        }
                    }
                    callFinished(false, "", oldCache, listener);
                } else {
                    callFinished(true, "Arbeitnow error " + response.code(), oldCache, listener);
                }
            }

            @Override
            public void onFailure(Call<ArbeitnowResponse> call, Throwable t) {
                callFinished(true, String.valueOf(t.getMessage()), oldCache, listener);
            }
        });
    }

    /** Called when one of the two web calls ends. When both are done, the result is saved and shown. */
    private void callFinished(boolean failed, String error, List<JobEntity> oldCache, Listener listener) {
        finishedCalls++;
        if (failed) {
            failedCalls++;
            lastError = error;
        }
        if (finishedCalls < 2) {
            return;
        }

        if (failedCalls == 2) {
            // no internet: show the older jobs from Room
            if (!oldCache.isEmpty()) {
                listener.onLoaded(toInternships(oldCache));
            } else {
                listener.onError(lastError);
            }
            return;
        }
        saveToCache(collected);
        listener.onLoaded(new ArrayList<>(collected));
    }

    /** Replaces the jobs in Room with the fresh ones. */
    private void saveToCache(List<Internship> jobs) {
        long now = System.currentTimeMillis();
        List<JobEntity> entities = new ArrayList<>();
        for (Internship job : jobs) {
            entities.add(JobEntity.from(job, now));
        }
        AppDatabase.databaseExecutor.execute(() -> {
            jobDao.deleteAll();
            jobDao.insertAll(entities);
        });
    }

    private List<Internship> toInternships(List<JobEntity> entities) {
        List<Internship> jobs = new ArrayList<>();
        for (JobEntity entity : entities) {
            jobs.add(entity.toInternship());
        }
        return jobs;
    }
}
