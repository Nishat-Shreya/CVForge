package com.example.cvforge.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The offline database: saved CVs. Room does not allow database calls on the main thread,
 * so every call goes through {@link #databaseExecutor}.
 */
@Database(entities = {CvEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    /** Runs database work on background threads. */
    public static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(2);

    public abstract CvDao cvDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "cvforge.db").build();
                }
            }
        }
        return instance;
    }
}
