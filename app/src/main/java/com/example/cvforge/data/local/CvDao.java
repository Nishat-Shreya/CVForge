package com.example.cvforge.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** Create, read, update and delete for the "cvs" table. Call these from a background thread. */
@Dao
public interface CvDao {

    /** Create. Replaces the row when the id already exists. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CvEntity cv);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CvEntity> cvs);

    /** Read one CV. */
    @Query("SELECT * FROM cvs WHERE id = :id")
    CvEntity getById(String id);

    /** Read all CVs of one student, newest first. */
    @Query("SELECT * FROM cvs WHERE ownerUid = :ownerUid ORDER BY updatedAt DESC")
    List<CvEntity> getAllForOwner(String ownerUid);

    @Update
    void update(CvEntity cv);

    @Delete
    void delete(CvEntity cv);

    @Query("DELETE FROM cvs WHERE ownerUid = :ownerUid")
    void deleteAllForOwner(String ownerUid);
}
