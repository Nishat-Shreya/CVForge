package com.example.cvforge.network;

import com.example.cvforge.network.model.GithubRepo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** GitHub REST API. No key is needed, but it allows only 60 requests per hour. */
public interface GithubApi {

    String BASE_URL = "https://api.github.com/";

    @GET("users/{user}/repos")
    Call<List<GithubRepo>> getRepos(
            @Path("user") String user,
            @Query("sort") String sort,
            @Query("per_page") int perPage);
}
