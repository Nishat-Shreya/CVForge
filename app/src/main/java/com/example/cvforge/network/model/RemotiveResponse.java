package com.example.cvforge.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of the Remotive API: {"jobs": [ ... ]}. */
public class RemotiveResponse {

    public List<Job> jobs = new ArrayList<>();

    public static class Job {
        public long id;
        public String url;
        public String title;
        public List<String> tags = new ArrayList<>();
        public String description;

        @SerializedName("company_name")
        public String companyName;

        @SerializedName("publication_date")
        public String publicationDate;

        @SerializedName("candidate_required_location")
        public String location;
    }
}
