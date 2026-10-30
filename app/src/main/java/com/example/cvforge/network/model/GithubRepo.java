package com.example.cvforge.network.model;

import com.google.gson.annotations.SerializedName;

/** One repository from the GitHub API. Gson fills the fields from the JSON. */
public class GithubRepo {

    public String name;
    public String description;
    public String language;
    public boolean fork;

    @SerializedName("stargazers_count")
    public int stars;

    @SerializedName("html_url")
    public String url;

    /** Ticked in the import list. transient means Gson ignores it. */
    public transient boolean selected;
}
