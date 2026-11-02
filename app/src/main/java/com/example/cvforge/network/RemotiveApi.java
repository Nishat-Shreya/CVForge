package com.example.cvforge.network;

import com.example.cvforge.network.model.RemotiveResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Remotive: real remote jobs, no key needed. Remotive asks apps to link back to the job
 * and to say that Remotive is the source, and not to ask more than a few times a day.
 */
public interface RemotiveApi {

    String BASE_URL = "https://remotive.com/";

    @GET("api/remote-jobs")
    Call<RemotiveResponse> getJobs(@Query("search") String search, @Query("limit") int limit);
}
