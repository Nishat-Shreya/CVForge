package com.example.cvforge.util;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.core.content.ContextCompat;

import com.example.cvforge.R;

import java.util.TimeZone;

/**
 * Puts an interview in the phone calendar with the Calendar Content Provider
 * (CalendarContract, W10 and W11). Two ways:
 * <ul>
 *   <li>insertEvent: writes the event directly with a ContentResolver. It needs the calendar
 *   permissions, which the screen asks for first.</li>
 *   <li>insertIntent: an implicit Intent (ACTION_INSERT). The calendar app opens with the event
 *   filled in and the user taps Save. It needs no permission and is the fallback.</li>
 * </ul>
 */
public class CalendarHelper {

    private static final long INTERVIEW_LENGTH_MS = 60L * 60 * 1000;
    /** The phone calendar reminds 30 minutes before the interview. */
    private static final int REMINDER_MINUTES = 30;

    private CalendarHelper() {
    }

    /** The permissions the direct insert needs. */
    public static String[] permissions() {
        return new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};
    }

    public static boolean hasPermission(Context context) {
        for (String permission : permissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes the interview into the first calendar of the phone that can be written to.
     * Returns false when there is no such calendar or the insert did not work.
     * Call it from the main thread only for such a small insert.
     */
    public static boolean insertEvent(Context context, String company, String jobTitle, long startMillis) {
        long calendarId = findWritableCalendar(context.getContentResolver());
        if (calendarId < 0) {
            return false;
        }

        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        event.put(CalendarContract.Events.TITLE, context.getString(R.string.calendar_event_title, jobTitle, company));
        event.put(CalendarContract.Events.DESCRIPTION, context.getString(R.string.calendar_event_description));
        event.put(CalendarContract.Events.DTSTART, startMillis);
        event.put(CalendarContract.Events.DTEND, startMillis + INTERVIEW_LENGTH_MS);
        event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

        Uri eventUri = context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, event);
        if (eventUri == null) {
            return false;
        }

        // a reminder in the calendar app, 30 minutes before
        ContentValues reminder = new ContentValues();
        reminder.put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(eventUri));
        reminder.put(CalendarContract.Reminders.MINUTES, REMINDER_MINUTES);
        reminder.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        context.getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, reminder);
        return true;
    }

    /** The id of the primary visible calendar that can be written to, or -1. */
    private static long findWritableCalendar(ContentResolver resolver) {
        String[] columns = {CalendarContract.Calendars._ID};
        String selection = CalendarContract.Calendars.VISIBLE + " = 1 AND "
                + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= "
                + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
        // the primary calendar comes first
        String order = CalendarContract.Calendars.IS_PRIMARY + " DESC";

        try (Cursor cursor = resolver.query(CalendarContract.Calendars.CONTENT_URI, columns, selection, null, order)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return -1;
    }

    /** Implicit Intent for the calendar app. The user checks the event and taps Save. */
    public static Intent insertIntent(Context context, String company, String jobTitle, long startMillis) {
        return new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, context.getString(R.string.calendar_event_title, jobTitle, company))
                .putExtra(CalendarContract.Events.DESCRIPTION, context.getString(R.string.calendar_event_description))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + INTERVIEW_LENGTH_MS);
    }
}
