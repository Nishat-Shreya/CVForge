package com.example.cvforge.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.cvforge.data.model.Internship;
import com.google.gson.Gson;

/** A job from another website, saved on the phone so the list opens without internet. */
@Entity(tableName = "jobs")
public class JobEntity {

    private static final Gson GSON = new Gson();

    @PrimaryKey
    @NonNull
    public String id = "";
    /** The whole job as JSON, so the table stays small and simple. */
    public String json;
    /** When the job was loaded from the website. */
    public long fetchedAt;

    public static JobEntity from(Internship job, long fetchedAt) {
        JobEntity entity = new JobEntity();
        entity.id = job.id;
        entity.json = GSON.toJson(job);
        entity.fetchedAt = fetchedAt;
        return entity;
    }

    public Internship toInternship() {
        return GSON.fromJson(json, Internship.class);
    }
}
