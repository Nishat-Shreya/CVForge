package com.example.cvforge.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.cvforge.R;

/**
 * A polite follow-up email for an application that got no reply (feature 13).
 * The text is a ready template. FollowUpFlow replaces it with a Gemini draft when it can.
 * The email app is opened with an implicit Intent (ACTION_SENDTO), and the student sends it.
 */
public class FollowUpEmail {

    private FollowUpEmail() {
    }

    public static String subject(Context context, String jobTitle) {
        return context.getString(R.string.followup_subject, jobTitle);
    }

    public static String body(Context context, String studentName, String company, String jobTitle) {
        return context.getString(R.string.followup_body, company, jobTitle, studentName);
    }

    /**
     * Implicit Intent for any email app. "mailto:" has no address, because the app does not
     * know the email address of the company, so the student types it.
     */
    public static Intent intent(Context context, String studentName, String company, String jobTitle) {
        return intent(context, studentName, company, jobTitle, body(context, studentName, company, jobTitle));
    }

    /** Same Intent, but with a given text (the AI draft) instead of the template. */
    public static Intent intent(Context context, String studentName, String company, String jobTitle, String text) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject(context, jobTitle));
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }
}
