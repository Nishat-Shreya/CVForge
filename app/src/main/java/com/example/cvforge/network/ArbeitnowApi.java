package com.example.cvforge.network;

import com.example.cvforge.network.model.ArbeitnowResponse;

import retrofit2.Call;
import retrofit2.http.GET;

/** Arbeitnow: job board with remote and on-site jobs, no key needed. */
public interface ArbeitnowApi {

    String BASE_URL = "https://www.arbeitnow.com/";

    @GET("api/job-board-api")
    Call<ArbeitnowResponse> getJobs();
}
