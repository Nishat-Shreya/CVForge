package com.example.cvforge.provider;

import android.net.Uri;

/**
 * The addresses and column names of CvProvider. Other code uses these constants
 * instead of typing the strings again.
 */
public class CvContract {

    /** Must match the authority in AndroidManifest.xml (applicationId + ".provider"). */
    public static final String AUTHORITY = "com.example.cvforge.provider";

    public static final Uri BOOKMARKS_URI = Uri.parse("content://" + AUTHORITY + "/bookmarks");
    public static final Uri CV_DRAFTS_URI = Uri.parse("content://" + AUTHORITY + "/cv_drafts");

    private CvContract() {
    }

    /** Columns of the bookmarks table. */
    public static class Bookmarks {
        public static final String INTERNSHIP_ID = "internshipId";
        public static final String OWNER_UID = "ownerUid";
        public static final String TITLE = "title";
        public static final String COMPANY_NAME = "companyName";
        public static final String LOCATION = "location";
        public static final String TYPE = "type";
        public static final String STIPEND = "stipend";
        public static final String DEADLINE = "deadline";
        public static final String SAVED_AT = "savedAt";
    }

    /** The only columns of the cvs table that are shared. The CV text itself stays private. */
    public static class CvDrafts {
        public static final String ID = "id";
        public static final String OWNER_UID = "ownerUid";
        public static final String TITLE = "title";
        public static final String UPDATED_AT = "updatedAt";
    }
}
