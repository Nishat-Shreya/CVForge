package com.example.cvforge.data;

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
 * Real jobs from other websites (Remotive and Arbeitnow), loaded with Retrofit.
 * The jobs are kept in memory for 6 hours, because Remotive asks apps not to call its API too
 * often. A later checkpoint keeps them in Room, so they also open offline.
 */
public class JobRepository {

    /** Callback with the jobs. Runs on the main thread. */
    public interface Listener {
        void onLoaded(List<Internship> jobs);

        void onError(String message);
    }

    /** The jobs in memory count as fresh for 6 hours. */
    private static final long FRESH_MS = 6L * 60 * 60 * 1000;

    // the memory is shared by all screens and lasts as long as the app process
    private static List<Internship> cachedJobs = new ArrayList<>();
    private static long cachedAt;

    private final RemotiveApi remotiveApi = ApiClient.create(RemotiveApi.BASE_URL, RemotiveApi.class);
    private final ArbeitnowApi arbeitnowApi = ApiClient.create(ArbeitnowApi.BASE_URL, ArbeitnowApi.class);

    // results of the two web calls (both callbacks run on the main thread, so no locking is needed)
    private final List<Internship> collected = new ArrayList<>();
    private int finishedCalls;
    private int failedCalls;
    private String lastError = "";

    /** Gives the jobs from memory when they are fresh, otherwise loads them from the websites. */
    public void loadExternalJobs(Listener listener) {
        long now = System.currentTimeMillis();
        if (!cachedJobs.isEmpty() && now - cachedAt < FRESH_MS) {
            listener.onLoaded(new ArrayList<>(cachedJobs));
            return;
        }
        loadFromWeb(listener);
    }

    private void loadFromWeb(Listener listener) {
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
                    callFinished(false, "", listener);
                } else {
                    callFinished(true, "Remotive error " + response.code(), listener);
                }
            }

            @Override
            public void onFailure(Call<RemotiveResponse> call, Throwable t) {
                callFinished(true, String.valueOf(t.getMessage()), listener);
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
                    callFinished(false, "", listener);
                } else {
                    callFinished(true, "Arbeitnow error " + response.code(), listener);
                }
            }

            @Override
            public void onFailure(Call<ArbeitnowResponse> call, Throwable t) {
                callFinished(true, String.valueOf(t.getMessage()), listener);
            }
        });
    }

    /** Called when one of the two web calls ends. When both are done, the result is kept and shown. */
    private void callFinished(boolean failed, String error, Listener listener) {
        finishedCalls++;
        if (failed) {
            failedCalls++;
            lastError = error;
        }
        if (finishedCalls < 2) {
            return;
        }
        // both calls failed: there is no internet or the websites are down
        if (failedCalls == 2) {
            listener.onError(lastError);
            return;
        }
        cachedJobs = new ArrayList<>(collected);
        cachedAt = System.currentTimeMillis();
        listener.onLoaded(new ArrayList<>(collected));
    }
}
