package com.example.cvforge.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The offline database: saved CVs, bookmarks and the cache of jobs from other websites.
 * Room does not allow database calls on the main thread, so every call goes through
 * {@link #databaseExecutor}.
 */
@Database(entities = {CvEntity.class, BookmarkEntity.class, JobEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** Version 1 to 2: adds the bookmarks table. The saved CVs stay as they are. */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` (`internshipId` TEXT NOT NULL, "
                    + "`ownerUid` TEXT NOT NULL, `title` TEXT, `companyName` TEXT, `location` TEXT, "
                    + "`type` TEXT, `stipend` INTEGER NOT NULL, `deadline` INTEGER NOT NULL, "
                    + "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`internshipId`, `ownerUid`))");
        }
    };

    /** Version 2 to 3: adds the jobs table. Everything else stays as it is. */
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `jobs` (`id` TEXT NOT NULL, `json` TEXT, "
                    + "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        }
    };

    private static volatile AppDatabase instance;

    /** Runs database work on background threads. */
    public static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(2);

    public abstract CvDao cvDao();

    public abstract BookmarkDao bookmarkDao();

    public abstract JobDao jobDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "cvforge.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }
}
