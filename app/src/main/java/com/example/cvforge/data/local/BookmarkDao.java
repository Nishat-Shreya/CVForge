package com.example.cvforge.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Create, read and delete for the "bookmarks" table. Call these from a background thread. */
@Dao
public interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookmarkEntity bookmark);

    @Query("SELECT * FROM bookmarks WHERE ownerUid = :ownerUid ORDER BY savedAt DESC")
    List<BookmarkEntity> getAllForOwner(String ownerUid);

    @Query("SELECT COUNT(*) FROM bookmarks WHERE ownerUid = :ownerUid AND internshipId = :internshipId")
    int count(String ownerUid, String internshipId);

    @Delete
    void delete(BookmarkEntity bookmark);
}
