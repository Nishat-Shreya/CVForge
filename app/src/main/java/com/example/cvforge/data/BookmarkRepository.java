package com.example.cvforge.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import com.example.cvforge.data.local.AppDatabase;
import com.example.cvforge.data.model.Internship;
import com.example.cvforge.provider.CvContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Bookmarked internships. Everything goes through our own Content Provider (CvProvider)
 * with a ContentResolver, on a background thread.
 */
public class BookmarkRepository {

    /** Callback with the bookmarked internships, newest first. Runs on the main thread. */
    public interface Listener {
        void onLoaded(List<Internship> internships);
    }

    private final ContentResolver resolver;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public BookmarkRepository(Context context) {
        resolver = context.getApplicationContext().getContentResolver();
    }

    public void add(String ownerUid, Internship post) {
        ContentValues values = new ContentValues();
        values.put(CvContract.Bookmarks.INTERNSHIP_ID, post.id);
        values.put(CvContract.Bookmarks.OWNER_UID, ownerUid);
        values.put(CvContract.Bookmarks.TITLE, post.title);
        values.put(CvContract.Bookmarks.COMPANY_NAME, post.posterName);
        values.put(CvContract.Bookmarks.LOCATION, post.location);
        values.put(CvContract.Bookmarks.TYPE, post.type);
        values.put(CvContract.Bookmarks.STIPEND, post.stipend);
        values.put(CvContract.Bookmarks.DEADLINE, post.deadline);
        values.put(CvContract.Bookmarks.SAVED_AT, System.currentTimeMillis());
        AppDatabase.databaseExecutor.execute(() -> resolver.insert(CvContract.BOOKMARKS_URI, values));
    }

    /** Removes one bookmark. The Runnable runs on the main thread when it is done. */
    public void remove(String ownerUid, String internshipId, Runnable onDone) {
        AppDatabase.databaseExecutor.execute(() -> {
            resolver.delete(CvContract.BOOKMARKS_URI,
                    CvContract.Bookmarks.OWNER_UID + " = ? AND " + CvContract.Bookmarks.INTERNSHIP_ID + " = ?",
                    new String[]{ownerUid, internshipId});
            mainHandler.post(onDone);
        });
    }

    public void loadAll(String ownerUid, Listener listener) {
        AppDatabase.databaseExecutor.execute(() -> {
            List<Internship> result = new ArrayList<>();
            try (Cursor cursor = resolver.query(CvContract.BOOKMARKS_URI, null,
                    CvContract.Bookmarks.OWNER_UID + " = ?", new String[]{ownerUid},
                    CvContract.Bookmarks.SAVED_AT + " DESC")) {
                while (cursor != null && cursor.moveToNext()) {
                    result.add(fromCursor(cursor));
                }
            }
            mainHandler.post(() -> listener.onLoaded(result));
        });
    }

    private Internship fromCursor(Cursor cursor) {
        Internship post = new Internship();
        post.id = cursor.getString(cursor.getColumnIndexOrThrow(CvContract.Bookmarks.INTERNSHIP_ID));
        post.title = cursor.getString(cursor.getColumnIndexOrThrow(CvContract.Bookmarks.TITLE));
        post.posterName = cursor.getString(cursor.getColumnIndexOrThrow(CvContract.Bookmarks.COMPANY_NAME));
        post.location = cursor.getString(cursor.getColumnIndexOrThrow(CvContract.Bookmarks.LOCATION));
        post.type = cursor.getString(cursor.getColumnIndexOrThrow(CvContract.Bookmarks.TYPE));
        post.stipend = cursor.getInt(cursor.getColumnIndexOrThrow(CvContract.Bookmarks.STIPEND));
        post.deadline = cursor.getLong(cursor.getColumnIndexOrThrow(CvContract.Bookmarks.DEADLINE));
        return post;
    }
}
