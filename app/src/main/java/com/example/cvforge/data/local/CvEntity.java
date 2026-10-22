package com.example.cvforge.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.cvforge.data.model.Cv;
import com.google.gson.Gson;

/**
 * Room row for one CV. The sections are stored together as JSON in one column,
 * so the table stays small and simple.
 */
@Entity(tableName = "cvs")
public class CvEntity {

    private static final Gson GSON = new Gson();

    @PrimaryKey
    @NonNull
    public String id = "";
    public String ownerUid;
    public String title;
    public long updatedAt;
    public String json;

    public static CvEntity fromCv(Cv cv) {
        CvEntity entity = new CvEntity();
        entity.id = cv.id;
        entity.ownerUid = cv.ownerUid;
        entity.title = cv.title;
        entity.updatedAt = cv.updatedAt;
        entity.json = GSON.toJson(cv);
        return entity;
    }

    public Cv toCv() {
        return GSON.fromJson(json, Cv.class);
    }
}
