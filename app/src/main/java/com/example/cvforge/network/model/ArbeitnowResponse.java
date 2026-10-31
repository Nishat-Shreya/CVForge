package com.example.cvforge.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of the Arbeitnow API: {"data": [ ... ]}. */
public class ArbeitnowResponse {

    public List<Job> data = new ArrayList<>();

    public static class Job {
        public String slug;
        public String title;
        public String url;
        public String location;
        public boolean remote;
        public List<String> tags = new ArrayList<>();
        public String description;

        @SerializedName("company_name")
        public String companyName;

        /** Seconds since 1970. */
        @SerializedName("created_at")
        public long createdAt;
    }
}
