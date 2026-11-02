package com.example.cvforge.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.example.cvforge.data.model.Internship;

/** An internship the student bookmarked. Saved on the phone so the list works offline. */
@Entity(tableName = "bookmarks", primaryKeys = {"internshipId", "ownerUid"})
public class BookmarkEntity {

    @NonNull
    public String internshipId = "";
    /** Several students can use one phone, so every bookmark belongs to one account. */
    @NonNull
    public String ownerUid = "";
    public String title;
    public String companyName;
    public String location;
    public String type;
    public int stipend;
    public long deadline;
    public long savedAt;

    public static BookmarkEntity from(String ownerUid, Internship post) {
        BookmarkEntity entity = new BookmarkEntity();
        entity.internshipId = post.id;
        entity.ownerUid = ownerUid;
        entity.title = post.title;
        entity.companyName = post.posterName;
        entity.location = post.location;
        entity.type = post.type;
        entity.stipend = post.stipend;
        entity.deadline = post.deadline;
        entity.savedAt = System.currentTimeMillis();
        return entity;
    }
}
