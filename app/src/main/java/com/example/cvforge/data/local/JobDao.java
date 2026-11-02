package com.example.cvforge.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Create, read and delete for the "jobs" table. Call these from a background thread. */
@Dao
public interface JobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<JobEntity> jobs);

    @Query("SELECT * FROM jobs")
    List<JobEntity> getAll();

    @Query("SELECT * FROM jobs WHERE id = :id")
    JobEntity getById(String id);

    /** Used before saving a fresh list, so jobs that are gone from the website disappear. */
    @Query("DELETE FROM jobs")
    void deleteAll();
}
