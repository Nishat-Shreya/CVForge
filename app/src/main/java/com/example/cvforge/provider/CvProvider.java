package com.example.cvforge.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;

import com.example.cvforge.data.local.AppDatabase;

/**
 * Our own Content Provider (W10). It shares the bookmarks (query, insert, delete) and the
 * list of CV drafts (query only) through content:// addresses.
 * The provider is not exported, so only this app can use it.
 */
public class CvProvider extends ContentProvider {

    private static final int BOOKMARKS = 1;
    private static final int CV_DRAFTS = 2;

    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(CvContract.AUTHORITY, "bookmarks", BOOKMARKS);
        MATCHER.addURI(CvContract.AUTHORITY, "cv_drafts", CV_DRAFTS);
    }

    private SupportSQLiteDatabase database() {
        return AppDatabase.getInstance(getContext()).getOpenHelper().getWritableDatabase();
    }

    @Override
    public boolean onCreate() {
        // the database is opened later, on first use, not here
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String table;
        switch (MATCHER.match(uri)) {
            case BOOKMARKS:
                table = "bookmarks";
                break;
            case CV_DRAFTS:
                table = "cvs";
                // never share the CV text, only these columns
                projection = new String[]{CvContract.CvDrafts.ID, CvContract.CvDrafts.OWNER_UID,
                        CvContract.CvDrafts.TITLE, CvContract.CvDrafts.UPDATED_AT};
                break;
            default:
                throw new IllegalArgumentException("Unknown address: " + uri);
        }
        SupportSQLiteQueryBuilder builder = SupportSQLiteQueryBuilder.builder(table)
                .columns(projection)
                .selection(selection, selectionArgs)
                .orderBy(sortOrder);
        Cursor cursor = database().query(builder.create());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (MATCHER.match(uri) != BOOKMARKS || values == null) {
            throw new IllegalArgumentException("Insert is only allowed for bookmarks: " + uri);
        }
        // replace when the student bookmarks the same internship again
        long rowId = database().insert("bookmarks", SQLiteDatabase.CONFLICT_REPLACE, values);
        if (rowId < 0) {
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, rowId);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (MATCHER.match(uri) != BOOKMARKS) {
            throw new IllegalArgumentException("Delete is only allowed for bookmarks: " + uri);
        }
        int deleted = database().delete("bookmarks", selection, selectionArgs);
        if (deleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return deleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        // nothing can be changed through the provider
        return 0;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (MATCHER.match(uri)) {
            case BOOKMARKS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + CvContract.AUTHORITY + ".bookmark";
            case CV_DRAFTS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + CvContract.AUTHORITY + ".cv_draft";
            default:
                return null;
        }
    }
}
